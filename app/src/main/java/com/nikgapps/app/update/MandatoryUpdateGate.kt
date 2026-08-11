package com.nikgapps.app.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nikgapps.app.utils.constants.ApplicationConstants
import com.nikgapps.app.utils.network.VersionFetcher
import com.nikgapps.dumps.getCurrentVersion
import com.nikgapps.dumps.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun MandatoryUpdateGate(
    enabled: Boolean,
    allowSettings: Boolean,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentVersion = remember { getCurrentVersion(context) }
    var checking by remember(enabled) { mutableStateOf(enabled) }
    var latestVersion by remember(enabled) { mutableStateOf<String?>(null) }
    var downloadId by remember { mutableStateOf<UUID?>(null) }
    var downloadState by remember { mutableStateOf<WorkInfo.State?>(null) }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        checking = true
        val latest = VersionFetcher.fetchLatestVersion()
        latestVersion = latest.takeUnless { it == "Unknown" }
        checking = false
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        val manager = WorkManager.getInstance(context)
        while (true) {
            val info = withContext(Dispatchers.IO) { manager.getWorkInfoById(id).get() }
            if (info == null) {
                delay(350)
                continue
            }
            downloadState = info.state
            if (info.state.isFinished) {
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    latestVersion?.let { version ->
                        installApk(context, AppUpdateManager.downloadedApk(context, version).absolutePath)
                    }
                }
                break
            }
            delay(350)
        }
    }

    val updateRequired = enabled && latestVersion?.let {
        VersionFetcher.isNewer(it, currentVersion)
    } == true
    val showGate = !allowSettings && enabled && (checking || updateRequired)
    BackHandler(enabled = showGate) {}

    Box(Modifier.fillMaxSize()) {
        content()
        if (showGate) {
            MandatoryUpdateScreen(
                checking = checking,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadState = downloadState,
                onUpdate = {
                    val version = latestVersion ?: return@MandatoryUpdateScreen
                    downloadId = AppUpdateManager.enqueueDownload(
                        context,
                        version,
                        ApplicationConstants.getNikGappsAppDownloadUrl(version)
                    )
                },
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun MandatoryUpdateScreen(
    checking: Boolean,
    currentVersion: String,
    latestVersion: String?,
    downloadState: WorkInfo.State?,
    onUpdate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val downloading = downloadState == WorkInfo.State.ENQUEUED || downloadState == WorkInfo.State.RUNNING
    Surface(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        ),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (checking || downloading) CircularProgressIndicator()
                    else Icon(Icons.Outlined.SystemUpdateAlt, null)
                    Text(
                        if (checking) "Checking for updates..." else "Update required",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (checking) "Confirming that NikGapps is up to date."
                        else "NikGapps $latestVersion is available. Update from $currentVersion to continue.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (!checking) {
                        FilledTonalButton(
                            onClick = onUpdate,
                            enabled = !downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (downloading) "Downloading..." else "Update now") }
                    }
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("NikGapps settings")
                    }
                }
            }
        }
    }
}
