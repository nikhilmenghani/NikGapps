package com.nikgapps.app.utils.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nikgapps.R
import com.nikgapps.app.data.BuildProjectRepository
import com.nikgapps.app.data.LatestBuildRepository
import com.nikgapps.app.data.BuildQuotaRepository
import com.nikgapps.app.registry.*
import java.io.File

class BuildZipWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val projectId = inputData.getString(KEY_PROJECT_ID).orEmpty()
    private val logFile = File(applicationContext.cacheDir, "build-runs/$projectId.log")

    override suspend fun doWork(): Result {
        setForeground(foreground("Preparing build", 0, 0))
        logFile.parentFile?.mkdirs(); logFile.writeText("")
        val project = BuildProjectRepository(applicationContext).getProjects().firstOrNull { it.id == projectId }
            ?: return failure("Project not found")
        val quota = BuildQuotaRepository(applicationContext)
        val quotaStatus = quota.status()
        if (!quotaStatus.allowed) return failure("Build limit reached. Try again after the current 6-hour window resets.")
        return try {
            progress("Loading package catalog", 0, project.selectedAppIds.size)
            val metadata = CatalogRepository(applicationContext.cacheDir).load(
                catalogAndroidVersion(project.androidVersion.displayName), project.defaultChannel,
                project.architecture.value)
            metadata.release?.let { log("Using release ${it.id} · ${it.createdAt.take(10)}") }
            val defaultChannel = ReleaseChannel.valueOf(project.defaultChannel.uppercase())
            val overrides = project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) }
            val selections = project.selectedAppIds.associateWith { id ->
                project.selectedPackageAppSets[id] ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
                ?: error("No AppSet owns selected package '$id'")
            }
            progress("Resolving packages and dependencies", 0, project.selectedAppIds.size)
            val resolution = CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
                selections, defaultChannel, overrides, project.androidVersion.apiLevel, project.architecture.value)
            val visibleTotal = resolution.packages.count { !it.hidden }
            var completed = 0
            val artifacts = mutableListOf<ValidatedArtifact>()
            val downloader = ArtifactDownloader(applicationContext.cacheDir)
            val validator = PackageZipValidator()
            resolution.packages.forEach { pkg ->
                val label = "Downloading ${pkg.catalogPackage.name}"
                progress(label, completed, visibleTotal, 0)
                var reported = -1
                val file = downloader.obtain(pkg) { download ->
                    val percent = download.total?.takeIf { it > 0 }?.let { (download.downloaded * 100 / it).toInt() }
                    if (percent != null && percent / 5 != reported / 5) {
                        reported = percent
                        progress(label, completed, visibleTotal, percent)
                    }
                }
                progress("Validating ${pkg.catalogPackage.name}", completed, visibleTotal)
                artifacts += ValidatedArtifact(pkg, file, validator.validate(file, pkg))
                if (!pkg.hidden) completed++
                progress("Prepared ${pkg.catalogPackage.name}", completed, visibleTotal)
            }
            progress("Assembling flashable ZIP", completed, visibleTotal)
            val primarySet = metadata.appSets.appSets.firstOrNull { it.id == project.selectedAppSetId }
                ?: metadata.appSets.appSets.first()
            val output = RegistryZipAssembler(AndroidBuilderAssetSource(applicationContext, metadata.builderAssets)).build(
                File(applicationContext.cacheDir, "zip-builds"), BuildRequest(
                    project.androidVersion.displayName, project.androidVersion.apiLevel,
                    project.architecture.value, primarySet, defaultChannel, overrides,
                    project.selectedAppIds, packageAppSets = resolution.packageAppSets,
                    projectName = project.name,
                    timestamp = metadata.release?.createdAt?.let(java.time.Instant::parse) ?: java.time.Instant.now(),
                    releaseId = metadata.release?.id
                ), artifacts)
            progress("Saving ZIP to Downloads/NikGapps", completed, visibleTotal)
            val publisher = ZipPublisher(applicationContext)
            if (publisher.exists(output.name)) {
                quota.recordSuccess()
                log("A ZIP named ${output.name} already exists")
                return Result.success(workDataOf(KEY_PENDING_SOURCE to output.absolutePath,
                    KEY_EXISTING_NAME to output.name))
            }
            val location = publisher.publish(output)
            quota.recordSuccess()
            LatestBuildRepository(applicationContext).save(projectId, location)
            output.delete()
            log("Build complete")
            Result.success(workDataOf(KEY_LOCATION to location))
        } catch (error: Exception) {
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

    private fun foreground(label: String, completed: Int, total: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "ZIP builds", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Building NikGapps ZIP")
            .setContentText(label).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(total.coerceAtLeast(1), completed.coerceAtLeast(0), total <= 0).build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_LABEL = "label"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
        const val KEY_PERCENT = "percent"
        const val KEY_LOCATION = "location"
        const val KEY_ERROR = "error"
        const val KEY_LOG_VERSION = "log_version"
        const val KEY_PENDING_SOURCE = "pending_source"
        const val KEY_EXISTING_NAME = "existing_name"
        const val CHANNEL_ID = "zip_builds"
        const val NOTIFICATION_ID = 3101
        fun uniqueName(projectId: String) = "build_zip_$projectId"
        fun logFile(context: Context, projectId: String) = File(context.cacheDir, "build-runs/$projectId.log")
    }
}
