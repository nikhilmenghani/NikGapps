package com.nikgapps.app.utils.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Settings {

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
