package com.nikgapps.app.utils.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Settings {
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        }
        context.startActivity(intent)
    }
}
