package com.nikgapps.app.presentation.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.AppSource
import com.nikgapps.app.data.AppSourceConfig
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.BuildProjectRepository
import com.nikgapps.app.data.SupportedApp
import com.nikgapps.app.data.SupportedApps
import com.nikgapps.app.utils.AppBuildInput
import com.nikgapps.app.utils.FlashableZipBuilder
import com.nikgapps.app.utils.ZipBuildProgress
import com.nikgapps.app.utils.ZipBuildResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DeviceAppStatus(val installed: Boolean, val version: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(projectId: String, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val repository = remember { BuildProjectRepository(context) }
    var project by remember(projectId) {
        mutableStateOf(repository.getProjects().firstOrNull { it.id == projectId })
    }
    val initialSelectedAppIds = remember(projectId) {
        project?.selectedAppIds.orEmpty()
    }
    val deviceStatuses = remember {
        SupportedApps.all.associate { app ->
            app.id to runCatching {
                @Suppress("DEPRECATION")
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        app.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    context.packageManager.getPackageInfo(app.packageName, 0)
                }
                DeviceAppStatus(true, info.versionName)
            }.getOrDefault(DeviceAppStatus(false, null))
        }
    }
    var pendingImportAppId by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val appId = pendingImportAppId
        pendingImportAppId = null
        if (uri != null && appId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            project?.let { current ->
                val updated = current.copy(
                    appSources = current.appSources + (
                        appId to AppSourceConfig(AppSource.IMPORTED, uri.toString())
                    )
                )
                repository.updateProject(updated)
                project = updated
            }
        }
    }
    val zipBuilder = remember { FlashableZipBuilder(context) }
    val scope = rememberCoroutineScope()
    var buildProgress by remember { mutableStateOf<ZipBuildProgress?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var buildFailed by remember { mutableStateOf(false) }

    val currentProject = project
    if (currentProject == null) {
        Text("Project not found", modifier = Modifier.padding(24.dp))
        return
    }

    LaunchedEffect(currentProject.id) {
        val validSelections = currentProject.selectedAppIds.filterTo(mutableSetOf()) { appId ->
            val source = currentProject.appSources[appId] ?: AppSourceConfig()
            when (source.source) {
                AppSource.DEVICE -> deviceStatuses[appId]?.installed == true
                else -> source.location.isNotBlank()
            }
        }
        if (validSelections != currentProject.selectedAppIds) {
            val updated = currentProject.copy(selectedAppIds = validSelections)
            repository.updateProject(updated)
            project = updated
        }
    }

    fun save(updated: BuildProject) {
        repository.updateProject(updated)
        project = updated
    }

    fun availableAppIds(project: BuildProject): Set<String> {
        return SupportedApps.all.mapNotNullTo(mutableSetOf()) { app ->
            val source = project.appSources[app.id] ?: AppSourceConfig()
            val available = when (source.source) {
                AppSource.DEVICE -> deviceStatuses[app.id]?.installed == true
                else -> source.location.isNotBlank()
            }
            app.id.takeIf { available }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject.name) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Supported apps", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Select an artifact source for each app. Device apps are selectable only when installed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            save(
                                currentProject.copy(
                                    selectedAppIds = availableAppIds(currentProject)
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null)
                        Text(" All")
                    }
                    TextButton(
                        onClick = {
                            save(currentProject.copy(selectedAppIds = emptySet()))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Deselect, contentDescription = null)
                        Text(" None")
                    }
                    TextButton(
                        onClick = {
                            save(
                                currentProject.copy(
                                    selectedAppIds =
                                        initialSelectedAppIds.intersect(
                                            availableAppIds(currentProject)
                                        )
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Text(" Revert")
                    }
                }
            }
            items(SupportedApps.all, key = { it.id }) { app ->
                val source = currentProject.appSources[app.id] ?: AppSourceConfig()
                val available = when (source.source) {
                    AppSource.DEVICE -> deviceStatuses[app.id]?.installed == true
                    else -> source.location.isNotBlank()
                }
                SupportedAppCard(
                    app = app,
                    source = source,
                    deviceStatus = deviceStatuses[app.id] ?: DeviceAppStatus(false, null),
                    selected = app.id in currentProject.selectedAppIds,
                    selectionEnabled = available,
                    onSelectedChange = { selected ->
                        save(
                            currentProject.copy(
                                selectedAppIds = if (selected) {
                                    currentProject.selectedAppIds + app.id
                                } else {
                                    currentProject.selectedAppIds - app.id
                                }
                            )
                        )
                    },
                    onSourceChange = { newSource ->
                        val config = AppSourceConfig(newSource)
                        save(
                            currentProject.copy(
                                appSources = currentProject.appSources + (app.id to config),
                                selectedAppIds = currentProject.selectedAppIds - app.id
                            )
                        )
                    },
                    onLocationChange = { location ->
                        save(
                            currentProject.copy(
                                appSources = currentProject.appSources + (
                                    app.id to source.copy(location = location)
                                )
                            )
                        )
                    },
                    onImport = {
                        pendingImportAppId = app.id
                        importLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                    }
                )
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            val inputs = SupportedApps.all
                                .filter { it.id in currentProject.selectedAppIds }
                                .map {
                                    AppBuildInput(
                                        it,
                                        currentProject.appSources[it.id] ?: AppSourceConfig()
                                    )
                                }
                            buildProgress = ZipBuildProgress(0, inputs.size, "Preparing project…")
                            when (
                                val result = zipBuilder.build(currentProject, inputs) { progress ->
                                    withContext(Dispatchers.Main) { buildProgress = progress }
                                }
                            ) {
                                is ZipBuildResult.Success -> {
                                    buildProgress = null
                                    buildFailed = false
                                    resultMessage = "Flashable ZIP created:\n${result.location}"
                                }
                                is ZipBuildResult.Failure -> {
                                    buildProgress = null
                                    buildFailed = true
                                    resultMessage = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null)
                    Text("  Build flashable ZIP (${currentProject.selectedAppIds.size} apps)")
                }
            }
        }
    }

    buildProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Building flashable ZIP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(progress.message)
                    Text("${progress.completed} of ${progress.total} apps")
                }
            },
            confirmButton = {}
        )
    }
    resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text(if (buildFailed) "Build failed" else "Build complete") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { resultMessage = null }) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportedAppCard(
    app: SupportedApp,
    source: AppSourceConfig,
    deviceStatus: DeviceAppStatus,
    selected: Boolean,
    selectionEnabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onSourceChange: (AppSource) -> Unit,
    onLocationChange: (String) -> Unit,
    onImport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (source.source) {
                            AppSource.DEVICE -> if (deviceStatus.installed) {
                                "Version ${deviceStatus.version ?: "Unknown"} · Device"
                            } else {
                                "Not installed · Device"
                            }
                            AppSource.IMPORTED -> "Imported APK"
                            else -> "Version resolved during build · ${source.source.displayName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectionEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Checkbox(
                    checked = selected && selectionEnabled,
                    onCheckedChange = onSelectedChange,
                    enabled = selectionEnabled
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Configure source"
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = sourceMenuExpanded,
                    onExpandedChange = { sourceMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = source.source.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(sourceMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        AppSource.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.displayName) },
                                onClick = {
                                    onSourceChange(it)
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                if (source.source == AppSource.IMPORTED) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                        Text(if (source.location.isBlank()) "Choose APK" else "Replace APK")
                    }
                } else if (
                    source.source == AppSource.GITLAB ||
                    source.source == AppSource.SOURCEFORGE
                ) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = source.location,
                        onValueChange = onLocationChange,
                        label = { Text("Direct APK URL") },
                        supportingText = { Text("Use a direct downloadable .apk link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
