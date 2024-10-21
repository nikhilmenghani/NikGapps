package com.nikgapps.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.nikgapps.ui.model.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun ListInstalledApps2(context: Context) {
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
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            items(installedApps) { appInfo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            color = if (appInfo.isSystemApp) Color(0xFFE0E0E0) else Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { /* Handle item click if needed */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = appInfo.appIcon as Painter,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appInfo.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = if (appInfo.isSystemApp) "System App" else "User App",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (appInfo.isSystemApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val installLocation: String,
    val appIcon: Painter,
    val isSystemApp: Boolean
)
