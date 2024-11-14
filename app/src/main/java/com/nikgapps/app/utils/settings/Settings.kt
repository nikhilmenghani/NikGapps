package com.nikgapps.app.utils.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

object Settings {
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun openAllFilesAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun openSettings(context: Context, action: String) {
        val intent = Intent(action).apply {
            when (action) {
                Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
                    putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                }
                else -> {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
        context.startActivity(intent)
    }
}
