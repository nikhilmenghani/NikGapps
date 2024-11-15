package com.nikgapps.app.utils.constants

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.nikgapps.app.data.model.PermissionInfo

object PermissionConstants {
    const val INSTALL_APPS = "Install Unknown Apps"
    const val STORAGE = "Storage"
    const val NOTIFICATIONS = "Notifications"
//    const val LOCATION = "Location"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val permissionMap = mapOf(
        NOTIFICATIONS to PermissionInfo(
            permission = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            rationale = "Notification permission is required to send you progress update notifications.",
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ),
        STORAGE to PermissionInfo(
            permission = arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
            rationale = "Storage permission is required to download and operate on NikGapps apk and zip files.",
            action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        ),
//        LOCATION to PermissionInfo(
//            permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//            rationale = "Location permission is required to access your location."
//        ),
        INSTALL_APPS to PermissionInfo(
            permission = arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
            rationale = "Install unknown apps permission is for seamless NikGapps app updates.",
            action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
        )
    )
}