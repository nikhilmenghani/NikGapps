package com.nikgapps.app.presentation.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.LocalActivity
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
import com.nikgapps.app.data.*
import com.nikgapps.app.registry.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class BuildStage { RUNNING, COMPLETE, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildZipScreen(projectId: String, navController: NavHostController) {
    val context = LocalActivity.current ?: return
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun log(message: String) { if (logs.lastOrNull() != message) logs += message }
    fun downloadProgress(packageName: String, percent: Long) {
        val prefix = "Downloading $packageName:"
        val message = "$prefix $percent%"
        val existing = logs.indexOfLast { it.startsWith(prefix) }
        if (existing >= 0) logs[existing] = message else logs += message
    }

    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex) }
    LaunchedEffect(projectId, retryKey) {
        logs.clear()
        completed = 0
        total = project?.selectedAppIds?.size ?: 0
        operationProgress = null
        operationLabel = "Preparing build"
        stage = BuildStage.RUNNING
        if (project == null) { stage = BuildStage.FAILED; log("Project not found"); return@LaunchedEffect }
        try {
            log("Loading the NikGapps package catalog…")
            operationLabel = "Loading package catalog"
            val metadata = withContext(Dispatchers.IO) { CatalogRepository(context.cacheDir).load() }
            log("Resolving ${project.selectedAppIds.size} selected apps and their dependencies…")
            operationLabel = "Resolving packages and dependencies"
            val defaultChannel = ReleaseChannel.STABLE
            val overrides = emptyMap<String, ReleaseChannel>()
            val selections = project.selectedAppIds.associateWith { id ->
                project.selectedPackageAppSets[id] ?: metadata.appSets.appSets.firstOrNull { id in it.packages }?.id
                ?: error("No AppSet owns selected package '$id'")
            }
            val resolution = withContext(Dispatchers.IO) { CatalogResolver(metadata.catalog, metadata.appSets).resolveAcrossAppSets(
                selections, defaultChannel, overrides, project.androidVersion.apiLevel, project.architecture.value) }
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
                    overrides, project.selectedAppIds, packageAppSets = resolution.packageAppSets), artifacts) }
            log("Saving ${output.name} to Downloads/NikGapps…")
            operationLabel = "Saving ZIP to Downloads/NikGapps"
            location = withContext(Dispatchers.IO) { ZipPublisher(context).publish(output) }
            LatestBuildRepository(context).save(projectId, location!!)
            output.delete()
            log("Build complete")
            stage = BuildStage.COMPLETE
        } catch (e: Exception) {
            log("Build failed: ${e.message ?: "Unknown error"}")
            stage = BuildStage.FAILED
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Build flashable ZIP") }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
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
                    Icon(when (stage) { BuildStage.COMPLETE -> Icons.Default.CheckCircle; BuildStage.FAILED -> Icons.Default.Error; else -> Icons.Default.Inventory2 }, null)
                    Spacer(Modifier.width(12.dp)); Column {
                        Text(when (stage) { BuildStage.COMPLETE -> "ZIP ready"; BuildStage.FAILED -> "Build failed"; else -> "Building ${project?.name.orEmpty()}" },
                            style = MaterialTheme.typography.titleMedium)
                        Text(location ?: "Keep this screen open while the package is created", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Build log", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (stage == BuildStage.RUNNING) Text("LIVE", style = MaterialTheme.typography.labelSmall,
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
                    .onSuccess { retryKey++ }
                    .onFailure { error -> log("Unable to clear cache: ${error.message}") }
            }
        }) { Text("Clear & rebuild") } }
    )
}

private fun Context.openNikGappsFolder() {
    val uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/NikGapps")
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { startActivity(intent) }.getOrElse {
        startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE))
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
