package com.nikgapps.app.presentation.ui.screen

import androidx.activity.compose.LocalActivity
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.BuildProjectRepository
import com.nikgapps.app.data.SupportedApp
import com.nikgapps.app.data.SupportedApps
import com.nikgapps.app.utils.FlashableZipBuilder
import com.nikgapps.app.utils.ZipBuildProgress
import com.nikgapps.app.utils.ZipBuildResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectId: String,
    navController: NavHostController
) {
    val context = LocalActivity.current ?: return
    val repository = remember { BuildProjectRepository(context) }
    var project by remember(projectId) {
        mutableStateOf(repository.getProjects().firstOrNull { it.id == projectId })
    }
    var selectedAppIds by remember(projectId) {
        mutableStateOf(project?.selectedAppIds.orEmpty())
    }
    val zipBuilder = remember { FlashableZipBuilder(context) }
    val coroutineScope = rememberCoroutineScope()
    var buildProgress by remember { mutableStateOf<ZipBuildProgress?>(null) }
    var buildResultMessage by remember { mutableStateOf<String?>(null) }
    var buildFailed by remember { mutableStateOf(false) }

    val currentProject = project
    if (currentProject == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Project not found") },
                    navigationIcon = {
                        IconButton(onClick = navController::navigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Text(
                "This project may have been deleted.",
                modifier = Modifier.padding(padding).padding(24.dp)
            )
        }
        return
    }

    fun setAppSelected(app: SupportedApp, selected: Boolean) {
        selectedAppIds = if (selected) {
            selectedAppIds + app.id
        } else {
            selectedAppIds - app.id
        }
        val updated = currentProject.copy(selectedAppIds = selectedAppIds)
        repository.updateProject(updated)
        project = updated
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Build target", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${currentProject.androidVersion.displayName} " +
                                "(API ${currentProject.androidVersion.apiLevel})"
                        )
                        Text(
                            "${currentProject.architecture.displayName} · " +
                                currentProject.architecture.value
                        )
                        Text(
                            "App source: Device",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Text("Supported apps", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Choose the apps to pull from this device when the flashable ZIP is built.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(SupportedApps.all, key = { it.id }) { app ->
                SupportedAppRow(
                    app = app,
                    selected = app.id in selectedAppIds,
                    onSelectedChange = { setAppSelected(app, it) }
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            buildFailed = false
                            buildResultMessage = null
                            buildProgress = ZipBuildProgress(
                                completed = 0,
                                total = selectedAppIds.size,
                                message = "Preparing project…"
                            )
                            val selectedApps = SupportedApps.all.filter {
                                it.id in selectedAppIds
                            }
                            when (
                                val result = zipBuilder.build(
                                    project = currentProject.copy(
                                        selectedAppIds = selectedAppIds
                                    ),
                                    apps = selectedApps
                                ) { progress ->
                                    withContext(Dispatchers.Main) {
                                        buildProgress = progress
                                    }
                                }
                            ) {
                                is ZipBuildResult.Success -> {
                                    buildProgress = null
                                    buildResultMessage =
                                        "Flashable ZIP created:\n${result.location}"
                                }
                                is ZipBuildResult.Failure -> {
                                    buildProgress = null
                                    buildFailed = true
                                    buildResultMessage = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null)
                    Text(
                        "  Build flashable ZIP (${selectedAppIds.size} apps)"
                    )
                }
                Text(
                    "Selected APKs and split APKs will be pulled from this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
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
    buildResultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { buildResultMessage = null },
            title = {
                Text(if (buildFailed) "Build failed" else "Build complete")
            },
            text = { Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { buildResultMessage = null }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SupportedAppRow(
    app: SupportedApp,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectedChange
            )
        }
    }
}
