package com.nikgapps.dumps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.App
import com.nikgapps.app.utils.network.VersionFetcher.fetchLatestVersion
import com.nikgapps.app.utils.worker.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("NewApi")
@Composable
fun UpdateAppCard() {
    val context = App.globalClass
    val workManager = WorkManager.getInstance(context)

    var currentVersion by remember { mutableStateOf(getCurrentVersion(context)) }
    var latestVersion by remember { mutableStateOf(currentVersion) }
    var isLatestVersion by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }

    // Launcher to navigate to settings to allow install from unknown sources
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // After navigating to settings, check permission again
        if (context.packageManager.canRequestPackageInstalls()) {
            installApk(context, "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps-v$latestVersion.apk")
        } else {
            Toast.makeText(context, "Permission not granted to install from unknown sources", Toast.LENGTH_LONG).show()
        }
    }

    // Fetch the latest version from GitHub API
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            latestVersion = fetchLatestVersion()
            isLatestVersion = (currentVersion == latestVersion)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatestVersion) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLatestVersion) {
                Text("On Latest Version v$currentVersion")
            } else {
                if (isDownloading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        isDownloading = true

                        val downloadUrl = "https://github.com/nikhilmenghani/nikgapps/releases/download/v$latestVersion/NikGapps-v$latestVersion.apk"
                        val destFilePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps-v$latestVersion.apk"

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
                                // Install the APK after download completes
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (context.packageManager.canRequestPackageInstalls()) {
                                        installApk(context, destFilePath)
                                    } else {
                                        // If permission not granted, navigate to settings to grant permission
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                                        settingsLauncher.launch(intent)
                                    }
                                } else {
                                    installApk(context, destFilePath)
                                }
                            } else if (info?.state == WorkInfo.State.FAILED) {
                                isDownloading = false
                                Toast.makeText(context, "Failed to download update", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Update to v$latestVersion available")
                    }
                }
            }
        }
    }
}

fun getCurrentVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: NameNotFoundException) {
        e.printStackTrace()
        "Unknown"
    }.toString()
}

fun installApk(context: Context, apkPath: String) {
    try {
        val apkFile = File(apkPath)
        if (apkFile.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            Log.e("NikGapps-UpdateAppCard", "APK file does not exist: $apkPath")
            Toast.makeText(context, "APK file does not exist: $apkPath", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("NikGapps-UpdateAppCard", "Error installing APK: ${e.message}")
        Toast.makeText(context, "Error installing APK: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Preview
@Composable
fun PreviewUpdateAppCard() {
    UpdateAppCard()
}
