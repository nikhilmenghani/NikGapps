package com.nikgapps.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.io.File

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val installLocation: String // New field for install location
)

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun ListInstalledApps1(context: Context) {
    // Remember the list of installed apps so it's not recomputed on every recomposition
    val installedApps = remember { getInstalledApps(context) }
    var originalSystemPaths = remember { getInstalledAppsWithOriginalPaths(context) }
    printPackageDetails(context, "com.android.vending")
    printPackageDetails(context, "com.nikgapps")

    // Displaying the list in a LazyColumn
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(installedApps) { app ->
            AppItem(app)
        }
    }
}

@Composable
fun AppItem(app: InstalledApp) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        app.icon?.let {
            Image(
                bitmap = it,
                contentDescription = "App Icon",
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = app.appName, style = MaterialTheme.typography.bodyLarge)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
            Text(text = "Installed at: ${app.installLocation}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun queryPackageInfo(context: Context, packageName: String): PackageInfo? {
    return try {
        // Query the package info using the package name
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    } catch (e: PackageManager.NameNotFoundException) {
        // If the package is not found, return null and show a message
        Toast.makeText(context, "Package $packageName not found", Toast.LENGTH_SHORT).show()
        null
    }
}

@RequiresApi(Build.VERSION_CODES.P)
fun printPackageDetails(context: Context, packageName: String) {
    val packageInfo = queryPackageInfo(context, packageName)

    packageInfo?.let {
        val versionName = it.versionName
        val versionCode = it.longVersionCode
        val installLocation = it.applicationInfo?.sourceDir

        // Display or log the package information
        println("Package Name: $packageName")
        println("Version Name: $versionName")
        println("Version Code: $versionCode")
        println("Installed at: $installLocation")
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager: PackageManager = context.packageManager
    val apps = mutableListOf<InstalledApp>()

    // Get a list of installed apps (including system apps)
    val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    // Iterate over the installed packages
    for (packageInfo in packages) {
        val appName = packageManager.getApplicationLabel(packageInfo).toString()
        val packageName = packageInfo.packageName
        val icon = packageManager.getApplicationIcon(packageInfo).toBitmap().asImageBitmap()

        // Get the install location (APK location)
        val installLocation = packageInfo.sourceDir

        // Add all apps (including system apps)
        apps.add(InstalledApp(appName, packageName, icon, installLocation))
    }

    return apps
}

fun getOriginalSystemPath(packageName: String): String? {
    // Define common system directories where system apps are typically located
    val systemAppDirs = listOf(
        "/system/app",
        "/system/priv-app",
        "/product/app",
        "/product/priv-app"
    )

    // Iterate over the directories to find the APK
    for (dir in systemAppDirs) {
        val file = File("$dir/$packageName")
        if (file.exists() && file.isDirectory) {
            val apkFiles = file.listFiles { _, name -> name.endsWith(".apk") }
            if (!apkFiles.isNullOrEmpty()) {
                return apkFiles[0].absolutePath // Return the APK path
            }
        }
    }

    return null // Return null if no APK is found in the system directories
}

@SuppressLint("QueryPermissionsNeeded")
fun getInstalledAppsWithOriginalPaths(context: Context): List<Pair<String, String?>> {
    val packageManager: PackageManager = context.packageManager
    val apps = mutableListOf<Pair<String, String?>>()

    val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    for (packageInfo in packages) {
        val packageName = packageInfo.packageName
        val currentInstallPath = packageInfo.sourceDir

        // If it's a system app (by checking the flags), try to find the original system location
        val originalSystemPath = if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            getOriginalSystemPath(packageName)
        } else {
            null // Not a system app
        }

        apps.add(Pair(currentInstallPath, originalSystemPath))
    }

    return apps
}