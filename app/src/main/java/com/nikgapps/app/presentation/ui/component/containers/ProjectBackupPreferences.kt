package com.nikgapps.app.presentation.ui.component.containers

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.saveable.rememberSaveable
import com.nikgapps.app.presentation.ui.component.items.PreferenceSubtitle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.app.registry.*
import com.nikgapps.BuildConfig
import com.nikgapps.app.data.*
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.network.EliteMembershipRepository
import com.nikgapps.app.utils.network.GitHubBuildAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectBackupPreferences() {
    val context = LocalContext.current
    val repository = remember(context) { BuildProjectRepository(context) }
    val scope = rememberCoroutineScope()
    var showBackupInfo by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ProjectBackup?>(null) }
    var existingProjects by remember { mutableStateOf(emptyList<BuildProject>()) }
    var restorePlan by remember { mutableStateOf<BackupRestorePlan?>(null) }
    var acknowledgeReplace by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var restoreElite by remember { mutableStateOf(false) }
    var verifiedEliteUsername by remember { mutableStateOf<String?>(null) }

    suspend fun verifyElite(): Boolean {
        val username = GithubPrefs.username
        if (GithubPrefs.token.isBlank() || username.isBlank()) return false
        return try {
            GitHubBuildAuth.requireBuildAccess()
            EliteMembershipRepository.isElite(username) && GithubPrefs.username == username
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { false }
    }
    var notice by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val text = exportText
        exportText = null
        if (uri != null && text != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open the backup file")
                    output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                notice = "Project backup saved. Open Settings → Account → Restore projects in the release build to import it."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { notice = "Could not save the backup. Choose another location and try again." }
            finally { busy = false }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val backup = withContext(Dispatchers.IO) {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst() && !cursor.isNull(0)) require(cursor.getLong(0) <= ProjectBackups.MAX_BYTES) {
                            "Backup exceeds 8 MiB"
                        }
                    }
                    val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open the backup file")
                    val bytes = input.use {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = it.read(buffer)
                            if (read < 0) break
                            require(output.size() + read <= ProjectBackups.MAX_BYTES) { "Backup exceeds 8 MiB" }
                            output.write(buffer, 0, read)
                        }
                        output.toByteArray()
                    }
                    ProjectBackups.read(bytes.toString(Charsets.UTF_8))
                }
                restorePlan = reviewBackupProjects(backup.projects, CatalogRepository(context.cacheDir))
                existingProjects = withContext(Dispatchers.IO) { repository.getProjects() }
                acknowledgeReplace = false
                selectedIds = restorePlan!!.compatible.mapTo(hashSetOf()) { it.id }
                restoreElite = verifyElite()
                verifiedEliteUsername = if (restoreElite) GithubPrefs.username else null
                restoreError = null
                preview = backup
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { notice = error.message ?: "This backup could not be read. No projects were changed." }
            finally { busy = false }
        }
    }

    Column {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            PreferenceSubtitle(text = "Project backup", contentPadding = PaddingValues(0.dp))
            IconButton(onClick = { showBackupInfo = !showBackupInfo }) {
                Icon(Icons.Default.Info, if (showBackupInfo) "Hide backup information" else "About project backup",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        AnimatedVisibility(visible = showBackupInfo) {
        Text("Move projects between debug and release builds using a .nikgapps file. Includes package choices, installer settings and debloater targets. Restore replaces all existing projects. Outdated projects are exported but cannot be imported. Sign-in tokens, APKs, ZIPs and build allowances are not included.",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PreferenceItem(title = if (busy) "Working…" else "Export projects", description = "Save a .nikgapps backup file",
            icon = Icons.Default.FileUpload, enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        exportText = withContext(Dispatchers.IO) {
                            ProjectBackups.export(repository.getProjects(), BackupSource(
                                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), BuildConfig.BUILD_TYPE))
                        }
                        val date = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        exportPicker.launch("NikGapps-projects-$date.nikgapps")
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { notice = error.message ?: "Could not prepare backup" }
                    finally { busy = false }
                }
            })
        PreferenceItem(title = "Restore projects", description = "Review and import a .nikgapps backup",
            icon = Icons.Default.FileDownload, enabled = !busy, onClick = { importPicker.launch(arrayOf("*/*")) })
    }
    preview?.let { backup ->
        val plan = restorePlan ?: return@let
        val canChooseProjects = restoreElite && GithubPrefs.username == verifiedEliteUsername && GithubPrefs.token.isNotBlank()
        Dialog(onDismissRequest = { if (!busy) preview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            val maxHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
                androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() * .85f
            }
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth(.94f).heightIn(max = maxHeight),
                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Restore projects", style = MaterialTheme.typography.headlineSmall)
                    Text("Created with NikGapps ${backup.source.appVersion} (${backup.source.buildType}) · Backup format ${backup.formatVersion}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Restore will replace all ${existingProjects.size} existing projects with ${if (canChooseProjects) selectedIds.size else plan.compatible.size} compatible projects from this backup. ZIP files must be rebuilt.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Outdated projects cannot be imported." + if (plan.outdated.isNotEmpty()) " ${plan.outdated.size} will be skipped." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plan.outdated.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (canChooseProjects) "Elite: choose the projects to restore." else
                        "All compatible projects are selected. Elite users can choose which projects to import.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        if (backup.projects.isEmpty()) item { Text("This backup contains no projects.") }
                        items(backup.projects, key = { it.id }) { project ->
                            val outdated = plan.outdated.any { it.id == project.id }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = !outdated && (project.id in selectedIds || !canChooseProjects),
                                    enabled = canChooseProjects && !outdated && !busy,
                                    onCheckedChange = {
                                        selectedIds = if (it) selectedIds + project.id else selectedIds - project.id
                                        acknowledgeReplace = false
                                    })
                                Column(Modifier.weight(1f)) {
                                    Text("${project.name} · ${project.androidVersion.displayName}", style = MaterialTheme.typography.titleSmall)
                                    Text(if (outdated) "Outdated · cannot be imported" else
                                        "${project.selectedAppIds.size} packages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (outdated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acknowledgeReplace, enabled = !busy && plan.compatible.isNotEmpty(),
                            onCheckedChange = { acknowledgeReplace = it })
                        Text("Replace all existing projects. This cannot be undone.", style = MaterialTheme.typography.bodySmall)
                    }
                    restoreError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(enabled = !busy, onClick = { preview = null },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
                        Button(enabled = !busy && acknowledgeReplace && plan.compatible.isNotEmpty() && (!canChooseProjects || selectedIds.isNotEmpty()), modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp), onClick = {
                                scope.launch {
                                    busy = true
                                    restoreError = null
                                    try {
                                        val latestPlan = reviewBackupProjects(backup.projects, CatalogRepository(context.cacheDir))
                                        if (latestPlan.compatible != plan.compatible || latestPlan.outdated != plan.outdated) {
                                            restorePlan = latestPlan
                                            selectedIds = latestPlan.compatible.mapTo(hashSetOf()) { it.id }
                                            acknowledgeReplace = false
                                            restoreError = "Package definitions changed. Review compatibility before replacing projects."
                                            return@launch
                                        }
                                        withContext(Dispatchers.IO) {
                                            val manager = androidx.work.WorkManager.getInstance(context)
                                            check(existingProjects.none { project -> manager.getWorkInfosForUniqueWork(
                                                com.nikgapps.app.utils.worker.BuildZipWorker.uniqueName(project.id)
                                            ).get().any { !it.state.isFinished } }) {
                                                "A ZIP build is still running. Wait for it to finish before restoring projects."
                                            }
                                        }
                                        val allIds = latestPlan.compatible.mapTo(hashSetOf()) { it.id }
                                        val requestedIds = if (canChooseProjects) selectedIds.toSet() else allIds
                                        val elite = verifyElite()
                                        if (!elite && requestedIds != allIds) {
                                            restoreElite = false
                                            verifiedEliteUsername = null
                                            selectedIds = allIds
                                            acknowledgeReplace = false
                                            restoreError = "Elite eligibility could not be confirmed. All compatible projects are now selected; review before restoring."
                                            return@launch
                                        }
                                        val count = withContext(Dispatchers.IO) {
                                            repository.replaceFromBackup(latestPlan.compatible, existingProjects, elite, requestedIds).also {
                                                LatestBuildRepository(context).clear()
                                            }
                                        }
                                        preview = null
                                        notice = "$count compatible projects restored. Previous projects were replaced." +
                                            if (latestPlan.outdated.isNotEmpty()) " ${latestPlan.outdated.size} outdated projects could not be imported." else ""
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (error: Exception) { restoreError = error.message ?: "Restore failed. No projects were changed." }
                                    finally { busy = false }
                                }
                            }) { Text(if (busy) "Restoring…" else "Replace & restore") }
                    }
                }
            }
        }
    }
    notice?.let { text -> AlertDialog(
        onDismissRequest = { notice = null },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Project backup", style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(onClick = { notice = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("OK", style = MaterialTheme.typography.labelLarge) }
            }
        }
    ) }

}
