package com.nikgapps.app.presentation.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.MainActivity
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.AndroidVersion
import com.nikgapps.app.data.Architecture
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.BuildProjectRepository
import com.nikgapps.app.data.LatestBuildRepository
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.presentation.navigation.projectRoute
import com.nikgapps.app.presentation.navigation.buildZipRoute
import com.nikgapps.app.registry.CatalogRepository
import com.nikgapps.app.registry.catalogAndroidVersion
import com.nikgapps.app.utils.constants.ApplicationConstants.getExternalStorageDir
import com.nikgapps.app.utils.constants.ApplicationConstants.getNikGappsAppDownloadUrl
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.app.utils.network.VersionFetcher.fetchLatestVersion
import com.nikgapps.app.utils.worker.DownloadWorker
import com.nikgapps.dumps.getCurrentVersion
import com.nikgapps.dumps.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalActivity.current as MainActivity
    val workManager = WorkManager.getInstance(context)
    val currentVersion = remember { getCurrentVersion(context) }
    var latestVersion by remember { mutableStateOf(currentVersion) }
    var isLatestVersion by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    val projectRepository = remember { BuildProjectRepository(context) }
    val latestBuildRepository = remember { LatestBuildRepository(context) }
    val reachabilitySpace = (LocalConfiguration.current.screenHeightDp * 0.18f).dp
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var projects by remember { mutableStateOf(projectRepository.getProjects()) }
    var showCreateProject by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<BuildProject?>(null) }
    var projectToDelete by remember { mutableStateOf<BuildProject?>(null) }

    LaunchedEffect(Unit) {
        latestVersion = withContext(Dispatchers.IO) { fetchLatestVersion() }
        isLatestVersion = currentVersion == latestVersion || latestVersion == "Unknown"
    }

    fun downloadUpdate() {
        isDownloading = true
        val destination = "${getExternalStorageDir()}/Download/NikGapps.apk"
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.DOWNLOAD_URL_KEY to getNikGappsAppDownloadUrl(latestVersion),
                    DownloadWorker.DEST_FILE_PATH_KEY to destination,
                    DownloadWorker.DOWNLOAD_TYPE_KEY to DownloadWorker.DOWNLOAD_TYPE_APK
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueue(request)
        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            when (info?.state) {
                WorkInfo.State.SUCCEEDED -> {
                    isDownloading = false
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        context.packageManager.canRequestPackageInstalls()
                    ) {
                        installApk(context, destination)
                    }
                }
                WorkInfo.State.FAILED -> {
                    isDownloading = false
                    Toast.makeText(context, "Failed to download update", Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "NikGapps",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "v$currentVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (!isLatestVersion) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(48.dp)
                        ) {
                            IconButton(
                                enabled = !isDownloading,
                                onClick = ::downloadUpdate,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.SystemUpdate,
                                        contentDescription = "Update to version $latestVersion"
                                    )
                                }
                            }
                            if (!isDownloading) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = 30.dp, y = 2.dp)
                                ) {
                                    Text("v$latestVersion")
                                }
                            }
                        }
                    }
                    IconButton(onClick = context::restartActivity) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                    IconButton(
                        onClick = {
                            navController.navigateWithState(Screens.Settings.name)
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = topBarScrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateProject = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create project")
            }
        }
    ) { paddingValues ->
        if (projects.isEmpty()) {
            EmptyProjects(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().height(reachabilitySpace), contentAlignment = Alignment.Center) {
                        Text("Projects", style = MaterialTheme.typography.headlineLarge)
                    }
                }
                items(projects, key = { it.id }) { project ->
                    val latestBuild = latestBuildRepository.get(project.id)
                    ProjectCard(
                        project = project,
                        onOpen = { navController.navigate(projectRoute(project.id)) },
                        onBuild = { navController.navigate(buildZipRoute(project.id)) },
                        onOpenZip = latestBuild?.let { saved -> { context.openPublishedZip(saved) } },
                        onDuplicate = {
                            projects = projectRepository.addProject(
                                BuildProject(
                                    name = "${project.name} copy",
                                    androidVersion = project.androidVersion,
                                    architecture = project.architecture,
                                    selectedAppSetId = project.selectedAppSetId,
                                    selectedPackageAppSets = project.selectedPackageAppSets,
                                    defaultChannel = project.defaultChannel,
                                    channelOverrides = project.channelOverrides,
                                    selectedAppIds = project.selectedAppIds,
                                    appSources = project.appSources
                                )
                            )
                        },
                        onEdit = { projectToEdit = project },
                        onDelete = { projectToDelete = project }
                    )
                }
            }
        }
    }

    if (showCreateProject) {
        ProjectSheet(
            onDismiss = { showCreateProject = false },
            onSave = { project ->
                projects = projectRepository.addProject(project)
                showCreateProject = false
                navController.navigate(projectRoute(project.id))
            }
        )
    }
    projectToEdit?.let { project ->
        ProjectSheet(
            project = project,
            onDismiss = { projectToEdit = null },
            onSave = { updatedProject ->
                projects = projectRepository.updateProject(updatedProject)
                projectToEdit = null
            }
        )
    }
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete project?") },
            text = { Text("\"${project.name}\" will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        projects = projectRepository.deleteProject(project.id)
                        latestBuildRepository.remove(project.id)
                        projectToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyProjects(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No projects yet", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap + to create a Google Apps project and choose its Android version and architecture.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: BuildProject,
    onOpen: () -> Unit,
    onBuild: () -> Unit,
    onOpenZip: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showProjectMenu by remember { mutableStateOf(false) }
    Box {
        Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showProjectMenu = true }
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${project.selectedAppIds.size} apps · ${
                            remember(project.createdAt) {
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                    .format(Date(project.createdAt))
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        project.androidVersion.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "API ${project.androidVersion.apiLevel} · ${project.architecture.displayName} build",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                ProjectActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = onDelete,
                    destructive = true
                )
                onOpenZip?.let { openZip ->
                    ProjectActionButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Open ZIP",
                        onClick = openZip
                    )
                }
                if (project.selectedAppIds.isNotEmpty()) {
                    ProjectActionButton(
                        icon = Icons.Default.Inventory2,
                        label = "Create ZIP",
                        onClick = onBuild
                    )
                }
            }
        }
    }
        DropdownMenu(
            expanded = showProjectMenu,
            onDismissRequest = { showProjectMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copy project") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    showProjectMenu = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text("Edit project") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    showProjectMenu = false
                    onEdit()
                }
            )
        }
    }
}

@Composable
private fun ProjectActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = CircleShape,
        colors = if (destructive) {
            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSheet(
    project: BuildProject? = null,
    onDismiss: () -> Unit,
    onSave: (BuildProject) -> Unit
) {
    val context = LocalActivity.current ?: return
    var name by remember(project) { mutableStateOf(project?.name.orEmpty()) }
    var androidVersion by remember(project) { mutableStateOf(project?.androidVersion ?: AndroidVersion.ANDROID_16) }
    val architecture = project?.architecture ?: Architecture.ARM64
    var versionMenuExpanded by remember { mutableStateOf(false) }
    var metadataVersions by remember { mutableStateOf(setOf(AndroidVersion.ANDROID_16)) }
    val developerPrefs = globalClass.preferencesManager.displayPrefs
    LaunchedEffect(Unit) {
        runCatching { CatalogRepository(context.cacheDir).load() }.getOrNull()?.let { metadata ->
            val published = metadata.releaseIndex?.releases?.map { it.androidVersion }?.toSet()
                ?: setOf(metadata.catalog.androidVersion)
            metadataVersions = AndroidVersion.entries.filterTo(mutableSetOf()) {
                catalogAndroidVersion(it.displayName) in published
            }
        }
    }
    val selectableVersions = if (developerPrefs.developerOptionsEnabled &&
        developerPrefs.allowUnsupportedAndroidVersions) AndroidVersion.entries else AndroidVersion.entries.filter { it in metadataVersions }
    LaunchedEffect(selectableVersions) {
        if (androidVersion !in selectableVersions && selectableVersions.isNotEmpty()) androidVersion = selectableVersions.last()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (project == null) "Create project" else "Edit project",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(expanded = versionMenuExpanded, onExpandedChange = { versionMenuExpanded = it }) {
                OutlinedTextField(value = "${androidVersion.displayName} (API ${androidVersion.apiLevel})",
                    onValueChange = {}, readOnly = true, label = { Text("Android version") },
                    supportingText = {
                        Text(if (developerPrefs.developerOptionsEnabled && developerPrefs.allowUnsupportedAndroidVersions)
                            "Developer override enabled" else "Versions with published metadata")
                    },
                    leadingIcon = { Icon(Icons.Default.Android, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(versionMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(expanded = versionMenuExpanded, onDismissRequest = { versionMenuExpanded = false }) {
                    selectableVersions.forEach { option ->
                        DropdownMenuItem(text = { Text("${option.displayName} (API ${option.apiLevel})") }, onClick = {
                            androidVersion = option
                            versionMenuExpanded = false
                        })
                    }
                }
            }
            OutlinedTextField(value = "${architecture.displayName} (${architecture.value})",
                onValueChange = {}, readOnly = true, label = { Text("Architecture") },
                supportingText = { Text("ARM64 is currently the supported architecture") },
                leadingIcon = { Icon(Icons.Default.Memory, null) }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onSave(
                        BuildProject(
                            id = project?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            androidVersion = androidVersion,
                            architecture = architecture,
                            selectedAppSetId = project?.selectedAppSetId ?: "core",
                            selectedPackageAppSets = project?.selectedPackageAppSets.orEmpty(),
                            defaultChannel = project?.defaultChannel ?: "stable",
                            channelOverrides = project?.channelOverrides.orEmpty()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (project == null) "Create project" else "Save changes")
            }
        }
    }
}
