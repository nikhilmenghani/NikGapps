package com.nikgapps.app.presentation.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.MainActivity
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.presentation.ui.component.buttons.UpdateIconButton
import com.nikgapps.app.presentation.ui.component.cards.InstallZipCard
import com.nikgapps.app.presentation.ui.component.cards.RootAccessCard
import com.nikgapps.app.utils.constants.ApplicationConstants.getExternalStorageDir
import com.nikgapps.app.utils.constants.ApplicationConstants.getNikGappsAppDownloadUrl
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.app.utils.fetchLatestVersion
import com.nikgapps.app.utils.worker.DownloadWorker
import com.nikgapps.dumps.getCurrentVersion
import com.nikgapps.dumps.installApk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current as MainActivity
    val workManager = WorkManager.getInstance(context)
    var currentVersion by remember { mutableStateOf(getCurrentVersion(context)) }
    var latestVersion by remember { mutableStateOf(currentVersion) }
    var isLatestVersion by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            latestVersion = fetchLatestVersion()
            isLatestVersion = (currentVersion == latestVersion)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "NikGapps") },
                actions = {
                    if (!isLatestVersion) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            UpdateIconButton(
                                versionNumber = latestVersion,
                                onClick = {
                                    isDownloading = true

                                    val downloadUrl = getNikGappsAppDownloadUrl(latestVersion)
                                    val destFilePath = "${getExternalStorageDir()}/Download/NikGapps.apk"

                                    val inputData = workDataOf(
                                        DownloadWorker.DOWNLOAD_URL_KEY to downloadUrl,
                                        DownloadWorker.DEST_FILE_PATH_KEY to destFilePath,
                                        DownloadWorker.DOWNLOAD_TYPE_KEY to DownloadWorker.DOWNLOAD_TYPE_APK
                                    )

                                    val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                        .setInputData(inputData)
                                        .setConstraints(
                                            Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build()
                                        )
                                        .build()

                                    // Enqueue the download request using WorkManager
                                    workManager.enqueue(downloadRequest)

                                    // Observe the WorkManager status
                                    workManager.getWorkInfoByIdLiveData(downloadRequest.id).observeForever { info ->
                                        if (info?.state == WorkInfo.State.SUCCEEDED) {
                                            isDownloading = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                if (context.packageManager.canRequestPackageInstalls()) {
                                                    installApk(context, destFilePath)
                                                }
                                            }
                                        } else if (info?.state == WorkInfo.State.FAILED) {
                                            isDownloading = false
                                            Toast.makeText(context, "Failed to download update", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        // Restart the activity to apply the new theme
                        context.restartActivity()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        navController.navigateWithState(
                            route = Screens.Settings.name
                        )
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                RootAccessCard()
                InstallZipCard(navController)
            }
        }
    )
}