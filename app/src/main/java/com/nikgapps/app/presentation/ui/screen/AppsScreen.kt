package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.InstalledAppInfo
import com.nikgapps.app.presentation.ui.component.cards.AppCard
import com.nikgapps.app.presentation.ui.component.layouts.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun AppsScreen() {
    val packageManager = globalClass.packageManager
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .map { app ->
            InstalledAppInfo(
                appName = app.loadLabel(packageManager).toString(),
                packageName = app.packageName,
                installLocation = app.sourceDir,
                appIcon = rememberAsyncImagePainter(model = app.loadIcon(packageManager)),
                isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                appType = if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && app.sourceDir.startsWith(
                        "/data/app"
                    )
                ) "Updated System App" else if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) "System App" else "User App"
            )
        }
        .sortedBy { it.appName }

    Scaffold(
        topBar = { AppTopBar(title = "Installed Apps") }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            items(installedApps) { appInfo ->
                AppCard(appInfo = appInfo, elevation = if (appInfo.isSystemApp) 2.dp else 60.dp)
            }
        }
    }
}
