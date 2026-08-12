package com.nikgapps.app.presentation.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.app.data.*
import com.nikgapps.app.registry.*
import com.nikgapps.app.utils.AppDiagnostics
import com.nikgapps.app.utils.worker.BuildZipWorker
import com.nikgapps.app.network.LocalInternetAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class BuildStage { RUNNING, AWAITING_FILE_CHOICE, COMPLETE, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildZipScreen(projectId: String, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val isOnline = LocalInternetAvailable.current
    val project = remember(projectId) { BuildProjectRepository(context).getProjects().firstOrNull { it.id == projectId } }
    val logs = remember { mutableStateListOf<String>() }
    var stage by remember { mutableStateOf(BuildStage.RUNNING) }
    var location by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(project?.selectedAppIds?.size ?: 0) }
    var operationProgress by remember { mutableStateOf<Float?>(null) }
    var operationLabel by remember { mutableStateOf("Preparing build") }
    var retryKey by remember { mutableIntStateOf(0) }
    var confirmClearCache by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<String?>(null) }
    var existingName by remember { mutableStateOf<String?>(null) }
    var activeRunId by remember { mutableStateOf("none") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val workManager = remember(context) { WorkManager.getInstance(context) }
    val workInfos by remember(workManager, projectId) {
        workManager.getWorkInfosForUniqueWorkFlow(BuildZipWorker.uniqueName(projectId))
    }.collectAsState(initial = emptyList())
    val workInfo = workInfos.lastOrNull()

    fun log(message: String) {
        if (logs.lastOrNull() != message) logs += message
        AppDiagnostics.info("build", "progress", mapOf("run" to activeRunId, "message" to message))
    }
    fun downloadProgress(packageName: String, percent: Long) {
        val prefix = "Downloading $packageName:"
        val message = "$prefix $percent%"
        val existing = logs.indexOfLast { it.startsWith(prefix) }
        if (existing >= 0) logs[existing] = message else logs += message
    }

    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex) }
    LaunchedEffect(workInfo?.state, workInfo?.progress, workInfo?.outputData) {
        val info = workInfo ?: return@LaunchedEffect
        BuildZipWorker.logFile(context, projectId).takeIf(File::isFile)?.readLines()?.let {
            logs.clear(); logs.addAll(it)
        }
        operationLabel = info.progress.getString(BuildZipWorker.KEY_LABEL) ?: operationLabel
        completed = info.progress.getInt(BuildZipWorker.KEY_COMPLETED, completed)
        total = info.progress.getInt(BuildZipWorker.KEY_TOTAL, total)
        operationProgress = info.progress.getInt(BuildZipWorker.KEY_PERCENT, -1)
            .takeIf { it >= 0 }?.div(100f)
        when (info.state) {
            WorkInfo.State.SUCCEEDED -> {
                pendingSource = info.outputData.getString(BuildZipWorker.KEY_PENDING_SOURCE)
                existingName = info.outputData.getString(BuildZipWorker.KEY_EXISTING_NAME)
                if (pendingSource != null) {
                    stage = BuildStage.AWAITING_FILE_CHOICE
                } else {
                    location = info.outputData.getString(BuildZipWorker.KEY_LOCATION)
                        ?: LatestBuildRepository(context).get(projectId)
                    stage = BuildStage.COMPLETE
                }
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                info.outputData.getString(BuildZipWorker.KEY_ERROR)?.let { if (logs.lastOrNull() != it) logs += it }
                stage = BuildStage.FAILED
            }
            else -> stage = BuildStage.RUNNING
        }
    }
    BackHandler(enabled = stage == BuildStage.RUNNING) { }
    LaunchedEffect(projectId, retryKey) {
        if (!isOnline) {
            stage = BuildStage.FAILED
            log("Internet connection is required before building the ZIP")
            return@LaunchedEffect
        }
        val request = OneTimeWorkRequestBuilder<BuildZipWorker>()
            .setInputData(workDataOf(BuildZipWorker.KEY_PROJECT_ID to projectId))
            .build()
        workManager.enqueueUniqueWork(
            BuildZipWorker.uniqueName(projectId),
            if (retryKey == 0) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
            request
        )
        if (retryKey < 0) {
        activeRunId = java.util.UUID.randomUUID().toString().take(8)
        logs.clear()
        completed = 0
        total = project?.selectedAppIds?.size ?: 0
        operationProgress = null
        operationLabel = "Preparing build"
        stage = BuildStage.RUNNING
        AppDiagnostics.info("build", "started", mapOf("run" to activeRunId,
            "project" to projectId.take(8), "selected" to (project?.selectedAppIds?.size ?: 0),
            "android" to project?.androidVersion?.displayName, "architecture" to project?.architecture?.value))
        if (project == null) { stage = BuildStage.FAILED; log("Project not found"); return@LaunchedEffect }
        try {
            log("Loading the NikGapps package catalog…")
            operationLabel = "Loading package catalog"
            val metadata = withContext(Dispatchers.IO) { CatalogRepository(context.cacheDir).load(
                catalogAndroidVersion(project.androidVersion.displayName), project.defaultChannel,
                project.architecture.value) }
            log("Resolving ${project.selectedAppIds.size} selected apps and their dependencies…")
            operationLabel = "Resolving packages and dependencies"
            val defaultChannel = ReleaseChannel.valueOf(project.defaultChannel.uppercase())
            val overrides = project.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) }
            val selections = project.selectedAppIds.associateWith { id ->
                project.selectedPackageAppSets[id] ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
                ?: error("No AppSet owns selected package '$id'")
            }
            val resolution = withContext(Dispatchers.IO) { CatalogResolver(metadata.catalog, metadata.appSets, metadata.release).resolveAcrossAppSets(
                selections, defaultChannel, overrides, project.androidVersion.apiLevel, project.architecture.value) }
            AppDiagnostics.info("build", "resolved", mapOf("run" to activeRunId,
                "selected" to project.selectedAppIds.size, "total" to resolution.packages.size,
                "dependencies" to resolution.packages.count { it.hidden },
                "packageIds" to resolution.packages.joinToString(",") { it.catalogPackage.id }))
            total = resolution.packages.count { !it.hidden }
            val artifacts = mutableListOf<ValidatedArtifact>()
            val downloader = ArtifactDownloader(context.cacheDir)
            val validator = PackageZipValidator()
            resolution.packages.forEach { pkg ->
                log(if (pkg.hidden) "Downloading required dependency ${pkg.catalogPackage.name}…"
                    else "Downloading ${pkg.catalogPackage.name}…")
                operationProgress = 0f
                operationLabel = "Downloading ${pkg.catalogPackage.name}"
                val file = downloader.obtain(pkg) { download -> withContext(Dispatchers.Main) {
                    val percent = download.total?.takeIf { it > 0 }?.let { download.downloaded * 100 / it }
                    if (percent != null) {
                        operationProgress = (percent / 100f).coerceIn(0f, 1f)
                        downloadProgress(pkg.catalogPackage.name, percent)
                    }
                } }
                log("Validating ${pkg.catalogPackage.name}…")
                operationProgress = null
                operationLabel = "Validating ${pkg.catalogPackage.name}"
                val artifact = withContext(Dispatchers.IO) { ValidatedArtifact(pkg, file, validator.validate(file, pkg)) }
                artifacts += artifact
                if (!pkg.hidden) completed++
                log("Prepared ${pkg.catalogPackage.name}")
            }
            log("Assembling the flashable ZIP…")
            operationProgress = null
            operationLabel = "Assembling flashable ZIP"
            val primarySet = metadata.appSets.appSets.firstOrNull { it.id == project.selectedAppSetId }
                ?: metadata.appSets.appSets.first()
            val output = withContext(Dispatchers.IO) { RegistryZipAssembler(AndroidBuilderAssetSource(context, metadata.builderAssets)).build(
                File(context.cacheDir, "zip-builds"), BuildRequest(project.androidVersion.displayName,
                    project.androidVersion.apiLevel, project.architecture.value, primarySet, defaultChannel,
                    overrides, project.selectedAppIds, packageAppSets = resolution.packageAppSets,
                    timestamp = metadata.release?.createdAt?.let(java.time.Instant::parse) ?: java.time.Instant.now(),
                    releaseId = metadata.release?.id), artifacts) }
            log("Saving ${output.name} to Downloads/NikGapps…")
            operationLabel = "Saving ZIP to Downloads/NikGapps"
            location = withContext(Dispatchers.IO) { ZipPublisher(context).publish(output) }
            LatestBuildRepository(context).save(projectId, location!!)
            output.delete()
            log("Build complete")
            AppDiagnostics.info("build", "succeeded", mapOf("run" to activeRunId,
                "packages" to artifacts.size, "destination" to "Downloads/NikGapps"))
            stage = BuildStage.COMPLETE
        } catch (e: Exception) {
            log("Build failed: ${e.message ?: "Unknown error"}")
            AppDiagnostics.failure("build", "failed", e, mapOf("run" to activeRunId,
                "stage" to operationLabel, "prepared" to completed, "total" to total))
            stage = BuildStage.FAILED
        }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Build flashable ZIP") }, navigationIcon = {
        IconButton(onClick = navController::navigateUp, enabled = stage != BuildStage.RUNNING) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
    }) }, bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (stage) {
                    BuildStage.RUNNING -> {
                        operationProgress?.let { value ->
                            LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
                        } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(operationProgress?.let { "${(it * 100).toInt()}% · $operationLabel" }
                            ?: "$operationLabel · $completed of $total apps prepared",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BuildStage.AWAITING_FILE_CHOICE -> Text(
                        "Choose whether to replace the existing ZIP or keep both files",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BuildStage.COMPLETE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        OutlinedButton(onClick = { context.openNikGappsFolder() }) {
                            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Open folder")
                        }
                        Button(onClick = { location?.let(context::openPublishedZip) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("Open ZIP")
                        }
                    }
                    BuildStage.FAILED -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        OutlinedButton(onClick = navController::navigateUp) { Text("Back") }
                        Button(onClick = { confirmClearCache = true }) {
                            Icon(Icons.Default.DeleteSweep, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear cache & rebuild")
                        }
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                color = when (stage) {
                    BuildStage.COMPLETE -> MaterialTheme.colorScheme.primaryContainer
                    BuildStage.FAILED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (stage) { BuildStage.COMPLETE, BuildStage.AWAITING_FILE_CHOICE -> Icons.Default.CheckCircle; BuildStage.FAILED -> Icons.Default.Error; else -> Icons.Default.Inventory2 }, null)
                    Spacer(Modifier.width(12.dp)); Column {
                        Text(when (stage) { BuildStage.COMPLETE -> "ZIP ready"; BuildStage.AWAITING_FILE_CHOICE -> "ZIP ready to save"; BuildStage.FAILED -> "Build failed"; else -> "Building ${project?.name.orEmpty()}" },
                            style = MaterialTheme.typography.titleMedium)
                        Text(location ?: "You can leave the app; the build will continue in the background",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Build log", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (stage == BuildStage.RUNNING) Text("RUNNING", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                LazyColumn(Modifier.fillMaxSize().padding(14.dp), state = listState,
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(logs) { entry ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("›", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(entry, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
    if (confirmClearCache) AlertDialog(
        onDismissRequest = { confirmClearCache = false },
        title = { Text("Clear build cache?") },
        text = { Text("Downloaded package metadata, package ZIPs, builder assets, and temporary build files will be removed. Saved projects and completed ZIPs are kept.") },
        dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            confirmClearCache = false
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { RegistryCache.clear(context.cacheDir) } }
                    .onSuccess { result ->
                        AppDiagnostics.info("cache", "cleared", mapOf("files" to result.files, "bytes" to result.bytes))
                        retryKey++
                    }
                    .onFailure { error ->
                        AppDiagnostics.failure("cache", "clear_failed", error)
                        log("Unable to clear cache: ${error.message}")
                    }
            }
        }) { Text("Clear & rebuild") } }
    )
    if (stage == BuildStage.AWAITING_FILE_CHOICE && pendingSource != null) AlertDialog(
        onDismissRequest = {},
        title = { Text("ZIP already exists") },
        text = { Text("$existingName already exists in Downloads/NikGapps. Do you want to replace it?") },
        dismissButton = { TextButton(onClick = {
            val source = pendingSource ?: return@TextButton
            scope.launch {
                runCatching { withContext(Dispatchers.IO) {
                    ZipPublisher(context).publish(File(source), ZipPublisher.ConflictResolution.RENAME)
                } }.onSuccess { saved ->
                    location = saved; LatestBuildRepository(context).save(projectId, saved)
                    File(source).delete(); pendingSource = null; stage = BuildStage.COMPLETE
                }.onFailure { error -> logs += "Unable to save ZIP: ${error.message}"; stage = BuildStage.FAILED }
            }
        }) { Text("Keep both") } },
        confirmButton = { TextButton(onClick = {
            val source = pendingSource ?: return@TextButton
            scope.launch {
                runCatching { withContext(Dispatchers.IO) {
                    ZipPublisher(context).publish(File(source), ZipPublisher.ConflictResolution.REPLACE)
                } }.onSuccess { saved ->
                    location = saved; LatestBuildRepository(context).save(projectId, saved)
                    File(source).delete(); pendingSource = null; stage = BuildStage.COMPLETE
                }.onFailure { error -> logs += "Unable to replace ZIP: ${error.message}"; stage = BuildStage.FAILED }
            }
        }) { Text("Replace") } }
    )
}

private fun Context.openNikGappsFolder() {
    val uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/NikGapps")
    val documentsPicker = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .setType("application/zip")
        .addCategory(Intent.CATEGORY_OPENABLE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
    if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
        startActivity(documentsPicker)
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { startActivity(intent) }.getOrElse {
        startActivity(documentsPicker)
    }
}

internal fun Context.openPublishedZip(location: String) {
    val uri: Uri? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        FileProvider.getUriForFile(this, "$packageName.provider", File(location))
    } else {
        val name = location.substringAfterLast('/')
        contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf(name), "${MediaStore.Downloads.DATE_ADDED} DESC")?.use { cursor ->
            if (cursor.moveToFirst()) Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0).toString()) else null
        }
    }
    uri?.let { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(it, "application/zip")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
}
