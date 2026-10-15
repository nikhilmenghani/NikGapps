package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nikgapps.app.data.ExplicitRelease
import com.nikgapps.app.network.LocalInternetAvailable
import com.nikgapps.app.utils.worker.DownloadWorker
import java.io.File
import java.util.UUID

@Composable
fun DownloadNikGappsCard() {
    val context = LocalContext.current
    val online = LocalInternetAvailable.current
    val manager = remember { WorkManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    var scheduling by remember { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var workId by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val selection = remember(url) { runCatching { ExplicitRelease.parse(url) }.getOrNull() }
    val info by remember(workId) {
        workId?.let { manager.getWorkInfoByIdFlow(UUID.fromString(it)) } ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)
    val downloading = scheduling || (workId != null && info?.state?.isFinished != true)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Download a release", style = MaterialTheme.typography.titleMedium)
            Text("Choose an explicit NikGapps release by pasting its HTTPS ZIP download link. No release is selected automatically.")
            OutlinedTextField(value = url, onValueChange = { url = it; message = null }, enabled = !downloading,
                label = { Text("Release ZIP URL") }, modifier = Modifier.fillMaxWidth())
            when {
                !online -> Text("Offline. Connect to load the selected release.")
                url.isBlank() -> Text("No release selected.")
                selection == null -> Text("Enter an HTTPS URL containing a valid .zip filename.", color = MaterialTheme.colorScheme.error)
                else -> Text("Selected: ${selection.fileName}")
            }
            if (downloading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (info?.state == WorkInfo.State.FAILED) Text("Release unavailable or download failed. Check the link and retry.", color = MaterialTheme.colorScheme.error)
            if (info?.state == WorkInfo.State.SUCCEEDED) Text("Download verified and saved to app downloads.")
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = online && selection != null && !downloading, shape = RoundedCornerShape(16.dp), onClick = {
                val selected = selection ?: return@Button
                val destination = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), selected.fileName)
                val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(workDataOf(
                    DownloadWorker.DOWNLOAD_URL_KEY to selected.url,
                    DownloadWorker.DEST_FILE_PATH_KEY to destination.path,
                    DownloadWorker.DOWNLOAD_TYPE_KEY to DownloadWorker.DOWNLOAD_TYPE_FILE))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
                scheduling = true
                scope.launch {
                    try {
                        workId = withContext(Dispatchers.IO) {
                            com.nikgapps.app.utils.worker.BuildWorkScheduler.enqueue(context,
                                "release-${selected.fileName}", "release-download-${java.util.UUID.randomUUID()}", request, true).toString()
                        }
                    } finally { scheduling = false }
                }
            }) { Text(if (downloading) "Downloading…" else "Download selected release") }
        }
    }
}
