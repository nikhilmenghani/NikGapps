package com.nikgapps.app.utils.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nikgapps.R
import com.nikgapps.app.data.BuildProjectRepository
import com.nikgapps.app.data.LatestBuildRepository
import com.nikgapps.app.data.BuildQuotaRepository
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.verifiedCompressionLevel
import com.nikgapps.app.data.verifiedZipFilenameTimestamp
import com.nikgapps.app.utils.network.GitHubBuildAuth
import com.nikgapps.app.registry.*
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.Date

class BuildZipWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val projectId = inputData.getString(KEY_PROJECT_ID).orEmpty()
    private val logFile = logFile(applicationContext, id.toString())

    override suspend fun doWork(): Result = RegistryCache.withLease { runBuild() }
    private suspend fun runBuild(): Result {
        setForeground(foreground("Preparing build", 0, 0))
        logFile.parentFile?.mkdirs(); logFile.writeText("")
        try {
            progress(if (GithubPrefs.guestMode && GithubPrefs.token.isBlank()) "Preparing guest build"
                else "Verifying GitHub account", 0, 0)
            GitHubBuildAuth.requireBuildAccess()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return authFailure(error.message ?: "GitHub sign-in is required before building a ZIP.")
        }
        val project = BuildProjectRepository(applicationContext).getProjects().firstOrNull { it.id == projectId }
            ?: return failure("Project not found")
        val quota = BuildQuotaRepository(applicationContext)
        val operations = applicationContext.getSharedPreferences("build-operations", Context.MODE_PRIVATE)
        (operations.getString("worker-published:$id", null) ?: operations.getString("publication:$id", null))?.let { saved ->
            quota.recordSuccess(operationId = id.toString())
            return Result.success(workDataOf(KEY_LOCATION to saved))
        }
        operations.getString("worker-output:$id", null)?.let { path ->
            val pending = File(path)
            if (!pending.isFile) return failure("Saved operation output is missing. Start a new build explicitly.")
            try { SafeFilePublication.validZip(pending) } catch (error: Exception) {
                return failure("Saved operation output is corrupt. Start a new build explicitly.", error)
            }
            return Result.success(workDataOf(KEY_PENDING_SOURCE to path, KEY_EXISTING_NAME to pending.name))
        }
        val quotaStatus = quota.status()
        if (!quotaStatus.allowed) {
            val resetTime = quotaStatus.resetsAtMillis?.let {
                DateFormat.getTimeFormat(applicationContext).format(Date(it))
            }
            return failure(resetTime?.let { "Build limit reached. All slots reset at $it." }
                ?: "Build limit reached.")
        }
        return try {
            val compressionLevel = verifiedCompressionLevel(
                inputData.getBoolean(KEY_COMPRESSED, false), inputData.getInt(KEY_COMPRESSION_LEVEL, 6))
            log(if (compressionLevel == 0) "Standard build: package compression off"
                else "Compressed build: level $compressionLevel (smaller ZIP, slower assembly)")
            progress("Loading package catalog", 0, 0)
            val repository = CatalogRepository(applicationContext.cacheDir)
            val latest = repository.load(
                catalogAndroidVersion(project.androidVersion.displayName), project.defaultChannel,
                project.architecture.value, forceRefresh = true)
            check(!latest.fromCache) { "Unable to verify the latest release. Please try again online." }
            check(projectUpdate(project, latest).kind != ProjectUpdateKind.INCOMPATIBLE) {
                "This configuration is outdated. Export it, fix incompatible entries and import it into a new project."
            }
            val baseline = project.lastBuiltReleaseId ?: project.createdReleaseId
            val pinnedMetadata = if (!inputData.getBoolean(KEY_LATEST_RELEASE, false) && baseline != null)
                repository.load(catalogAndroidVersion(project.androidVersion.displayName), project.defaultChannel,
                    project.architecture.value, releaseId = baseline) else latest
            val metadata = if ((project.configVersion?.toIntOrNull() ?: 0) >
                (pinnedMetadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE]?.configVersion?.toIntOrNull() ?: 0)) latest else pinnedMetadata
            metadata.release?.let { log("Using release ${it.createdAt.take(10)}") }
            val defaultChannel = ReleaseChannel.valueOf(project.defaultChannel.uppercase())
            val overrides = project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) }
            val selections = project.selectedAppIds.associateWith { id ->
                project.selectedPackageAppSets[id] ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
                ?: error("No AppSet owns selected package '$id'")
            }
            progress("Resolving packages and dependencies", 0, 0)
            val resolution = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
                selections, defaultChannel, overrides, project.androidVersion.apiLevel, project.architecture.value)
            val packageTotal = resolution.packages.size
            DebloaterConfig.validate(project.debloatTargets,
                DebloaterConfig.conflicts(resolution.packages.map { it.catalogPackage }, project.keepAospCounterparts))
            StoragePreflight.check(applicationContext.cacheDir, resolution.packages.map { it.version.artifact.size })
            var completed = 0
            val artifacts = mutableListOf<ValidatedArtifact>()
            val downloader = ArtifactDownloader(applicationContext.cacheDir)
            val validator = PackageZipValidator()
            resolution.packages.forEach { pkg ->
                val label = "Downloading ${pkg.catalogPackage.name}"
                progress(label, completed, packageTotal, 0)
                var reported = -1
                val file = downloader.obtain(pkg) { download ->
                    val percent = download.total?.takeIf { it > 0 }?.let { (download.downloaded * 100 / it).toInt() }
                    if (percent != null && percent / 5 != reported / 5) {
                        reported = percent
                        progress(label, completed, packageTotal, percent)
                    }
                }
                progress("Validating ${pkg.catalogPackage.name}", completed, packageTotal)
                artifacts += ValidatedArtifact(pkg, file, validator.validate(file, pkg))
                completed++
                progress("Prepared ${pkg.catalogPackage.name}", completed, packageTotal)
            }
            progress("Assembling flashable ZIP", completed, packageTotal)
            GitHubBuildAuth.requireBuildAccess()
            val primarySet = metadata.appSets.appSets.firstOrNull { it.id == project.selectedAppSetId }
                ?: metadata.appSets.appSets.first()
            val output = RegistryZipAssembler(AndroidBuilderAssetSource(applicationContext, metadata.builderAssets)).build(
                File(applicationContext.filesDir, "build-operations"), BuildRequest(
                    project.androidVersion.displayName, project.androidVersion.apiLevel,
                    project.architecture.value, primarySet, defaultChannel, overrides,
                    project.selectedAppIds, packageAppSets = resolution.packageAppSets,
                    projectName = project.name,
                    timestamp = metadata.release?.createdAt?.let(java.time.Instant::parse) ?: java.time.Instant.now(),
                    releaseId = metadata.release?.id, compressionLevel = compressionLevel,
                    keepAospCounterparts = project.keepAospCounterparts, installerSettings = project.installerSettings,
                    debloatTargets = project.debloatTargets,
                    packageDefinitions = metadata.builderAssets[RegistryZipAssembler.CONFIG_TEMPLATE]?.packageDefinitions.orEmpty().filterKeys { it in project.selectedAppIds },
                    filenameTimestamp = verifiedZipFilenameTimestamp()
                ), artifacts)
            check(operations.edit().putString("worker-output:$id", output.absolutePath).commit())
            try {
                GitHubBuildAuth.requireBuildAccess()
            } catch (error: Exception) {
                output.delete()
                throw error
            }
            progress("Saving ZIP to Downloads/NikGapps", completed, packageTotal)
            val publisher = ZipPublisher(applicationContext)
            if (publisher.exists(output.name)) {
                log("A ZIP named ${output.name} already exists")
                return Result.success(workDataOf(KEY_PENDING_SOURCE to output.absolutePath,
                    KEY_EXISTING_NAME to output.name))
            }
            val location = publisher.publish(output, project.selectedAppIds.size, operationId = id.toString())
            check(operations.edit().putString("worker-published:$id", location).commit())
            quota.recordSuccess(operationId = id.toString())
            LatestBuildRepository(applicationContext).save(projectId, location)
            recordSuccessfulProjectZip(applicationContext, project, output)
            output.delete()
            log("Build complete")
            Result.success(workDataOf(KEY_LOCATION to location))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (error is GitHubBuildAuth.AuthException) {
                return authFailure(error.message ?: "GitHub sign-in is required before building a ZIP.")
            }
            failure("Build failed: ${error.message ?: "Unknown error"}", error)
        }
    }

    private suspend fun progress(label: String, completed: Int, total: Int, percent: Int = -1) {
        log(label)
        setProgress(workDataOf(KEY_LABEL to label, KEY_COMPLETED to completed,
            KEY_TOTAL to total, KEY_PERCENT to percent, KEY_LOG_VERSION to logFile.length()))
        setForeground(foreground(label, completed, total))
    }

    private fun log(message: String) {
        if (logFile.takeIf(File::isFile)?.readLines()?.lastOrNull() != message)
            logFile.appendText(message + "\n")
    }

    private fun failure(message: String, error: Throwable? = null): Result {
        log(message)
        return Result.failure(workDataOf(KEY_ERROR to message, KEY_LOG_VERSION to logFile.length()))
    }

    private fun authFailure(message: String): Result {
        log(message)
        return Result.failure(workDataOf(KEY_ERROR to message, KEY_AUTH_ERROR to true,
            KEY_LOG_VERSION to logFile.length()))
    }

    private fun foreground(label: String, completed: Int, total: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "ZIP builds", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Building NikGapps ZIP")
            .setContentText(label).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(total.coerceAtLeast(1), completed.coerceAtLeast(0), total <= 0).build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            if (android.os.Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
    }

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_LATEST_RELEASE = "latest_release"
        const val KEY_COMPRESSED = "compressed"
        const val KEY_COMPRESSION_LEVEL = "compression_level"
        const val KEY_LABEL = "label"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
        const val KEY_PERCENT = "percent"
        const val KEY_LOCATION = "location"
        const val KEY_ERROR = "error"
        const val KEY_AUTH_ERROR = "auth_error"
        const val KEY_LOG_VERSION = "log_version"
        const val KEY_PENDING_SOURCE = "pending_source"
        const val KEY_EXISTING_NAME = "existing_name"
        const val CHANNEL_ID = "zip_builds"
        const val NOTIFICATION_ID = 3101
        fun uniqueName(projectId: String) = "build_zip_$projectId"
        fun logFile(context: Context, projectId: String) = File(context.cacheDir, "build-runs/$projectId.log")
    }
}
