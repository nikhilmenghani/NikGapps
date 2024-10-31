package com.nikgapps.ui.components.cards

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.utils.DownloadUtility
import com.nikgapps.utils.fetchLatestVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.nikgapps.App

@SuppressLint("NewApi")
@Composable
fun UpdateAppCard() {
    val context = App.globalClass
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
                        CoroutineScope(Dispatchers.IO).launch {
                            val downloadUrl = "https://github.com/nikhilmenghani/nikgapps/releases/download/v$latestVersion/NikGapps-v$latestVersion.apk"
                            val destFilePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps-v$latestVersion.apk"
                            val downloadSuccess = DownloadUtility.downloadApk(downloadUrl, destFilePath)

                            withContext(Dispatchers.Main) {
                                if (downloadSuccess) {
                                    // Check if permission to install unknown apps is granted
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
                                } else {
                                    Toast.makeText(context, "Failed to download update", Toast.LENGTH_LONG).show()
                                }
                                isDownloading = false
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
                "${context.packageName}.provider", // Update this with your FileProvider authority
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            Log.e("UpdateAppCard", "APK file does not exist: $apkPath")
            Toast.makeText(context, "APK file does not exist: $apkPath", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("UpdateAppCard", "Error installing APK: ${e.message}")
        Toast.makeText(context, "Error installing APK: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Preview
@Composable
fun PreviewUpdateAppCard() {
    UpdateAppCard()
}
