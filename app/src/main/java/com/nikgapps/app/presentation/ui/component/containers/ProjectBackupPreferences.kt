package com.nikgapps.app.presentation.ui.component.containers

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var includeOutdated by remember { mutableStateOf(false) }
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
                selectedIds = restorePlan!!.compatible.mapTo(hashSetOf()) { it.id }
                restoreElite = verifyElite()
                verifiedEliteUsername = if (restoreElite) GithubPrefs.username else null
                restoreError = null
                includeOutdated = false
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
        Text("Move projects between debug and release builds using a .nikgapps file. Includes package choices, installer settings and debloater targets. Restore replaces all existing projects. Outdated projects are exported; Elite users can optionally restore them. Sign-in tokens, APKs, ZIPs and build allowances are not included.",
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
        ProjectRestoreScreen(
            backup = backup, plan = plan, existingCount = existingProjects.size,
            selectedIds = if (canChooseProjects) selectedIds else plan.compatible.mapTo(hashSetOf()) { it.id },
            canChooseProjects = canChooseProjects, includeOutdated = canChooseProjects && includeOutdated,
            onIncludeOutdated = { enabled ->
                includeOutdated = enabled
                val outdatedIds = plan.outdated.mapTo(hashSetOf()) { it.id }
                selectedIds = if (enabled) selectedIds + outdatedIds else selectedIds - outdatedIds
            }, busy = busy, error = restoreError,
            onSelection = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
            onDismiss = { if (!busy) preview = null },
            onRestore = {
                scope.launch {
                    busy = true
                    restoreError = null
                    try {
                        val latestPlan = reviewBackupProjects(backup.projects, CatalogRepository(context.cacheDir))
                        if (latestPlan.compatible != plan.compatible || latestPlan.outdated != plan.outdated) {
                            restorePlan = latestPlan
                            selectedIds = latestPlan.reviewedSelection(selectedIds, canChooseProjects, includeOutdated)
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
                        val candidates = latestPlan.compatible + if (canChooseProjects && includeOutdated) latestPlan.outdated else emptyList()
                        val requestedIds = if (canChooseProjects) selectedIds.toSet() else allIds
                        val elite = verifyElite()
                        if (!elite && (requestedIds != allIds || includeOutdated)) {
                            includeOutdated = false
                            restoreElite = false
                            verifiedEliteUsername = null
                            selectedIds = allIds
                            restoreError = "Elite eligibility could not be confirmed. All compatible projects are now selected; review before restoring."
                            return@launch
                        }
                        val count = withContext(Dispatchers.IO) {
                            repository.replaceFromBackup(candidates, existingProjects, elite, requestedIds).also {
                                LatestBuildRepository(context).clear()
                            }
                        }
                        preview = null
                        notice = "$count projects restored. Previous projects were replaced."
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { restoreError = error.message ?: "Restore failed. No projects were changed." }
                    finally { busy = false }
                }
            }
        )
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
