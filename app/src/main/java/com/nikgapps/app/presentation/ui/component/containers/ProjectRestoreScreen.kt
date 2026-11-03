package com.nikgapps.app.presentation.ui.component.containers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.data.ProjectBackup
import com.nikgapps.app.registry.BackupRestorePlan
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun ProjectRestoreScreen(backup: ProjectBackup, plan: BackupRestorePlan, existingCount: Int,
    selectedIds: Set<String>, canChooseProjects: Boolean, includeOutdated: Boolean,
    onIncludeOutdated: (Boolean) -> Unit, busy: Boolean, error: String?,
    onSelection: (String) -> Unit, onDismiss: () -> Unit, onRestore: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val projectDateFormat = remember(locale) { SimpleDateFormat("MMM d, yyyy", locale) }
    var showInfo by remember(backup) { mutableStateOf(false) }
    var showWarnings by remember(backup) { mutableStateOf(false) }
    var confirmOverwrite by remember(backup) { mutableStateOf(false) }
    LaunchedEffect(error) { if (error != null) showWarnings = true }
    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Restore projects", style = MaterialTheme.typography.headlineSmall)
                        Text("${selectedIds.size} selected", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(Icons.Default.Info, if (showInfo) "Hide restore information" else "Restore information",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showWarnings = !showWarnings }) {
                        BadgedBox(badge = { if (plan.outdated.isNotEmpty() || error != null) Badge() }) {
                            Icon(Icons.Default.Warning, if (showWarnings) "Hide restore warnings" else "Restore warnings",
                                tint = if (plan.outdated.isNotEmpty() || error != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Notices scroll with the project list rather than squeezing the bottom actions off screen.
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "restore-info") {
                        AnimatedVisibility(showInfo, enter = expandVertically(expandFrom = Alignment.Top),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top)) {
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Created with NikGapps ${backup.source.appVersion} (${backup.source.buildType}) · Backup format ${backup.formatVersion}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text(if (canChooseProjects) "Tap a project to include or exclude it from restore." else
                                        "All compatible projects are selected. Elite users can choose which projects to import.",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("Restoring replaces the current project list. ZIP files must be rebuilt.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item(key = "restore-warnings") {
                        AnimatedVisibility(showWarnings, enter = expandVertically(expandFrom = Alignment.Top),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top)) {
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (canChooseProjects) "Outdated projects may need updating before they can be built. Enable Import outdated projects to include them."
                                        else "Outdated projects cannot be imported. ${plan.outdated.size} will be skipped.",
                                        style = MaterialTheme.typography.bodySmall)
                                    if (existingCount > 0) Text("All $existingCount existing projects will be replaced after confirmation. This cannot be undone.",
                                        style = MaterialTheme.typography.bodySmall)
                                    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    if (backup.projects.isEmpty()) item { Text("This backup contains no projects.") }
                    items(backup.projects, key = { it.id }) { project ->
                        val outdated = plan.outdated.any { it.id == project.id }
                        val selected = project.id in selectedIds && (!outdated || includeOutdated)
                        Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(enabled = canChooseProjects && (!outdated || includeOutdated) && !busy,
                            role = Role.Checkbox, onClickLabel = "Select ${project.name}", onClick = { onSelection(project.id) })
                            .semantics { toggleableState = if (selected) ToggleableState.On else ToggleableState.Off },
                            shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.medium,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primaryContainer
                                        outdated -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    }) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(if (outdated) Icons.Default.Warning else Icons.Outlined.FolderSpecial,
                                            if (outdated) if (selected) "Selected outdated project" else "Outdated project" else if (selected) "Selected project" else "Project not selected",
                                            tint = when {
                                                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                outdated -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            })
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(project.name, Modifier.weight(1f).alignByBaseline(), style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(project.androidVersion.displayName, Modifier.alignByBaseline(), style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${project.selectedAppIds.size} apps · ${projectDateFormat.format(Date(project.createdAt))}",
                                            Modifier.weight(1f).alignByBaseline(), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("API ${project.androidVersion.apiLevel} · ${project.architecture.displayName}",
                                            Modifier.alignByBaseline(), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                if (canChooseProjects && plan.outdated.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .toggleable(value = includeOutdated, enabled = !busy, role = Role.Checkbox,
                                onValueChange = onIncludeOutdated),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, if (includeOutdated) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Import outdated projects", style = MaterialTheme.typography.titleSmall)
                                Text("${plan.outdated.size} outdated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(checked = includeOutdated, onCheckedChange = null, enabled = !busy)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(enabled = !busy, onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
                    Button(enabled = !busy && selectedIds.isNotEmpty(), onClick = {
                        if (existingCount > 0) confirmOverwrite = true else onRestore()
                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                        Text(if (busy) "Restoring…" else "Restore")
                    }
                }
            }
        }
        if (confirmOverwrite) AlertDialog(onDismissRequest = { confirmOverwrite = false },
            containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
            shape = RoundedCornerShape(24.dp), title = { Text("Replace existing projects?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Restore will replace all $existingCount existing projects with ${selectedIds.size} projects from this backup. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { confirmOverwrite = false }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
                        Button(onClick = { confirmOverwrite = false; onRestore() },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) { Text("Restore") }
                    }
                }
            })
    }
}
