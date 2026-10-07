package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.InstalledAppInfo
import com.nikgapps.app.presentation.ui.component.cards.AppCard
import com.nikgapps.app.presentation.ui.component.layouts.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

private data class LoadedInstalledApp(val name: String, val packageName: String,
    val sourceDir: String, val icon: android.graphics.drawable.Drawable,
    val systemApp: Boolean, val type: String)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun AppsScreen() {
    val packageManager = globalClass.packageManager
    var installedApps by remember { mutableStateOf<List<LoadedInstalledApp>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(packageManager) {
        try {
            installedApps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map { app ->
                    val systemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    LoadedInstalledApp(app.loadLabel(packageManager).toString(), app.packageName,
                        app.sourceDir, app.loadIcon(packageManager), systemApp,
                        if (systemApp && app.sourceDir.startsWith("/data/app")) "Updated System App"
                        else if (systemApp) "System App" else "User App")
                }.sortedBy { it.name }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            loadError = error.message ?: "Unable to load installed apps"
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Installed Apps") }
    ) { paddingValues ->
        val loaded = installedApps
        if (loaded == null) Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            if (loadError != null) Text(loadError!!) else CircularProgressIndicator()
        } else LazyColumn(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            items(loaded, key = { it.packageName }) { app ->
                val appInfo = InstalledAppInfo(app.name, app.packageName, app.sourceDir,
                    rememberAsyncImagePainter(model = app.icon), app.systemApp, app.type)
                AppCard(appInfo = appInfo, elevation = if (appInfo.isSystemApp) 2.dp else 60.dp)
            }
        }
    }
}
