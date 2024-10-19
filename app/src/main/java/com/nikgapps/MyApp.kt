package com.nikgapps

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.nikgapps.ui.theme.NikGappsTheme
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MyApp(context: Context) {
    NikGappsTheme {
        ListInstalledApps(context)
    }
}

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun ListInstalledApps(context: Context) {
    val packageManager = context.packageManager
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
        .map { it.loadLabel(packageManager).toString() }
        .sorted()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(installedApps) { appName ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(text = appName)
            }
        }
    }
}