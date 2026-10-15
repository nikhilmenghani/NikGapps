package com.nikgapps.app.presentation.ui.component.cards

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nikgapps.app.presentation.ui.component.bottomsheets.InstallZipProgressBottomSheet
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.BuildUtility
import com.nikgapps.app.utils.SafeZipExtractor
import com.nikgapps.app.utils.runInstallation
import com.nikgapps.app.utils.managers.ResourceManager
import com.nikgapps.app.utils.managers.ScriptManager
import com.nikgapps.app.utils.root.RootManager
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val installationLock = Mutex()

@Composable
fun InstallZipCard(progressLogViewModel: ProgressLogViewModel) {
    var isProcessing by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && !isProcessing) scope.launch {
            isProcessing = true; showBottomSheet = true
            val workspace = File(context.cacheDir, "installation-${UUID.randomUUID()}")
            try {
                withContext(Dispatchers.IO) {
                    check(workspace.mkdirs())
                    val archive = File(workspace, "archive.zip")
                    (context.contentResolver.openInputStream(uri) ?: error("Cannot open ZIP")).use { input ->
                        archive.outputStream().use { output ->
                            val buffer = ByteArray(32768); var total = 0L
                            while (true) { currentCoroutineContext().ensureActive(); val count = input.read(buffer); if (count < 0) break
                                total += count; check(total <= 4L * 1024 * 1024 * 1024) { "Archive too large" }; output.write(buffer, 0, count) }
                        }
                    }
                    installZipFile(context, progressLogViewModel, archive)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { progressLogViewModel.addLog("Installation failed: ${error.message}") }
            finally { withContext(NonCancellable + Dispatchers.IO) { workspace.deleteRecursively() }; isProcessing = false }
        }
    }
    ActionCard(title = "Install NikGapps", description = "Pick a NikGapps ZIP file and install it on your device. Requires root access.",
        buttonText = "Install ZIP", icon = Icons.Default.Download, isProcessing = isProcessing,
        onClick = { if (!isProcessing) picker.launch("application/zip") })
    if (showBottomSheet) InstallZipProgressBottomSheet(viewModel = progressLogViewModel,
        onDismiss = { showBottomSheet = false }, isProcessing = isProcessing)
}

suspend fun installZipFile(context: Context, progressLogViewModel: ProgressLogViewModel, file: File,
    progressCallback: (Boolean) -> Unit = {}) = installationLock.withLock {
    val workspace = File(context.filesDir, "root-install-${UUID.randomUUID()}")
    withContext(Dispatchers.Main) { progressCallback(true) }
    try {
        withContext(Dispatchers.IO) {
            check(workspace.mkdirs())
            val root = RootManager(context)
            check(runInterruptible { root.hasRootAccess() }) { "Root access required" }
            val archive = File(workspace, "archive.zip")
            file.copyTo(archive)
            val resources = ResourceManager(context)
            var sets = emptyList<com.nikgapps.app.data.AppSet>()
            runInstallation(extract = {
                SafeZipExtractor.extract(archive, File(workspace, "archive"), nested = true)
                sets = BuildUtility.scanForApps(progressLogViewModel, archive.path)
                check(sets.isNotEmpty() && sets.any { it.packages.isNotEmpty() }) { "No packages found" }
                // Validate every package before mounting or performing any system writes.
                sets.flatMap { it.packages }.forEach { it.getInstallScript("") }
                true
            }, mount = {
                ScriptManager.createScriptFile(File(workspace, "variables.sh").path, resources.getScript("variables"))
                val mount = ScriptManager.createScriptFile(File(workspace, "mount.sh").path, resources.getScript("mount"))
                runInterruptible { root.executeScriptAsRoot(mount.path).success }
            }, install = {
                sets.all { currentCoroutineContext().ensureActive()
                    runInterruptible { BuildUtility.installAppSet(progressLogViewModel, it, root, resources, workspace.path) } }
            })
            currentCoroutineContext().ensureActive()
            progressLogViewModel.addLog("Installed NikGapps successfully")
        }
    } finally {
        withContext(NonCancellable + Dispatchers.IO) { workspace.deleteRecursively() }
        withContext(NonCancellable + Dispatchers.Main) { progressCallback(false) }
    }
}
