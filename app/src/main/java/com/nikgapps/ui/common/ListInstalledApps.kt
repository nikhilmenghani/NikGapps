package com.nikgapps.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.nikgapps.ui.model.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun ListInstalledApps(context: Context) {
    val packageManager = context.packageManager
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .map { app ->
            InstalledAppInfo(
                appName = app.loadLabel(packageManager).toString(),
                packageName = app.packageName,
                installLocation = app.sourceDir,
                appIcon = rememberAsyncImagePainter(app.loadIcon(packageManager)),
                isSystemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
        .sortedBy { it.appName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Installed Apps") },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            items(installedApps) { appInfo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (appInfo.isSystemApp) {
                            Text(
                                text = "System App",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "User App",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = appInfo.appIcon as Painter,
                                contentDescription = "App Icon",
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = 16.dp)
                            )
                            Column {
                                Text(
                                    text = appInfo.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Package: ${appInfo.packageName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Install Location: ${appInfo.installLocation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (appInfo.isSystemApp) "Type: System App" else "Type: User App",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
