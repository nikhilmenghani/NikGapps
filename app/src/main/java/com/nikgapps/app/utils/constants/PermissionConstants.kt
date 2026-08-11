package com.nikgapps.app.utils.constants

import android.Manifest
import android.os.Build
import android.provider.Settings
import com.nikgapps.app.data.PermissionInfo

object PermissionConstants {
    const val INSTALL_APPS = "Install Unknown Apps"
    const val STORAGE = "Storage"
    const val NOTIFICATIONS = "Notifications"
//    const val LOCATION = "Location"

    val permissionMap: Map<String, PermissionInfo>
        get() = buildMap {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                put(NOTIFICATIONS, PermissionInfo(
                    permission = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    rationale = "Notification permission is required to send you progress update notifications.",
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                ))
            }
            put(STORAGE, PermissionInfo(
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                },
                rationale = "Storage permission is required to download and operate on NikGapps apk and zip files.",
                action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                } else {
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                }
            ))
//        LOCATION to PermissionInfo(
//            permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//            rationale = "Location permission is required to access your location."
//        ),
            put(INSTALL_APPS, PermissionInfo(
                permission = arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
                rationale = "Install unknown apps permission is for seamless NikGapps app updates.",
                action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
            ))
        }
}
