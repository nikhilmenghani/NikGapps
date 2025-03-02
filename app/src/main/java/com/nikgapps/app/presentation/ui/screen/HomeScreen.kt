package com.nikgapps.app.presentation.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.MainActivity
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.bottomsheets.CustomBottomSheet
import com.nikgapps.app.presentation.ui.component.buttons.FilledTonalButtonWithIcon
import com.nikgapps.app.presentation.ui.component.cards.DeviceStatsCard
import com.nikgapps.app.utils.constants.ApplicationConstants.getExternalStorageDir
import com.nikgapps.app.utils.constants.ApplicationConstants.getNikGappsAppDownloadUrl
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.app.utils.network.GitHubApi.createOrUpdateFile
import com.nikgapps.app.utils.network.GitHubApi.fetchJsonFile
import com.nikgapps.app.utils.network.VersionFetcher.fetchLatestVersion
import com.nikgapps.app.utils.worker.DownloadWorker
import com.nikgapps.dumps.getCurrentVersion
import com.nikgapps.dumps.installApk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController
) {
    val context = LocalActivity.current as MainActivity
    val workManager = WorkManager.getInstance(context)
    val currentVersion by remember { mutableStateOf(getCurrentVersion(context)) }
    var latestVersion by remember { mutableStateOf(currentVersion) }
    var isLatestVersion by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            latestVersion = fetchLatestVersion()
            isLatestVersion = (currentVersion == latestVersion) || (latestVersion == "Unknown")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "NikGapps") },
                actions = {
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
        floatingActionButton = {
            if (!isLatestVersion) {
                FloatingActionButton(
                    onClick = {
                        isDownloading = true

                        val downloadUrl = getNikGappsAppDownloadUrl(latestVersion)
                        val destFilePath =
                            "${getExternalStorageDir()}/Download/NikGapps.apk"

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
                        workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                            .observeForever { info ->
                                if (info?.state == WorkInfo.State.SUCCEEDED) {
                                    isDownloading = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (context.packageManager.canRequestPackageInstalls()) {
                                            installApk(context, destFilePath)
                                        }
                                    }
                                } else if (info?.state == WorkInfo.State.FAILED) {
                                    isDownloading = false
                                    Toast.makeText(
                                        context,
                                        "Failed to download update",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Available\nNikGapps v$latestVersion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            Column {
                Row(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    FilledTonalButtonWithIcon(onClick = {
                        showBottomSheet = true
                    },
                        icon = Icons.Default.DeviceUnknown,
                        text = "Device Stats"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalButtonWithIcon(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                createOrUpdateFile(
                                    token = globalClass.preferencesManager.githubPrefs.token,
                                    commitMessage = "Update latest version",
                                    newFileContent = "{ \"test\" : \"test\" }"
                                )
                            }
                        }
                    },
                        icon = Icons.Default.DataArray,
                        text = "Sync Up Data"
                    )
                }
                HorizontalDivider()
                DisplayFileContent(
                    token = globalClass.preferencesManager.githubPrefs.token,
                    filePath = "folder_access.json"
                )
            }
        }
    )
    if (showBottomSheet) {
        CustomBottomSheet(
            title = "Device Stats",
            sheetContent = { DeviceStatsCard() },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
fun DisplayFileContent(token: String, filePath: String) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dataMap by remember { mutableStateOf<Map<String, String>?>(null) }

    LaunchedEffect(filePath) {
        fetchJsonFile(token, filePath) { result ->
            if (result.startsWith("Error:") || result == "File doesn't exist") {
                errorMessage = result
            } else {
                try {
                    val jsonObject = JSONObject(result)
                    val map = mutableMapOf<String, String>()
                    jsonObject.keys().forEach { key ->
                        map[key] = jsonObject.getString(key)
                    }
                    dataMap = map
                } catch (e: Exception) {
                    errorMessage = "Error parsing JSON: ${e.message}"
                }
            }
        }
    }

    // Wrap everything in a Box to ensure bounded height constraints.
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            errorMessage != null -> {
                Text(text = errorMessage!!, modifier = Modifier.padding(16.dp))
            }
            dataMap != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // You can add header items here if needed as separate items {}
                    items(dataMap!!.toList()) { (folder, username) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = folder,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = username,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            else -> {
                Text("Loading...", modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    NikGappsThemePreview {
        FilledTonalButtonWithIcon(
            onClick = {},
            icon = Icons.Default.DataArray,
            text = "Click Me"
        )
    }
}
