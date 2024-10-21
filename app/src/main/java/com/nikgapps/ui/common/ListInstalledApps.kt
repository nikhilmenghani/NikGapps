package com.nikgapps.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.nikgapps.ui.components.cards.AppCard
import com.nikgapps.ui.components.topbars.AppTopBar
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
                appIcon = rememberAsyncImagePainter(model = app.loadIcon(packageManager)),
                isSystemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                appType = if ((app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    app.sourceDir.startsWith(
                        "/data/app"
                    )
                ) "Updated System App"
                else if ((app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
                    "System App"
                else "User App"
            )
        }
        .sortedBy { it.appName }

    Scaffold(
        topBar = { AppTopBar(title = "Installed Apps") }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(installedApps) { appInfo ->
                AppCard(appInfo = appInfo, elevation = if (appInfo.isSystemApp) 2.dp else 60.dp)
            }
        }
    }
}
