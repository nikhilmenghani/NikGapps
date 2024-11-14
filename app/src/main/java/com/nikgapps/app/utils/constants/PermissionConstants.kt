package com.nikgapps.app.utils.constants

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import com.nikgapps.app.data.model.PermissionInfo

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val permissionMap = mapOf(
    "Notifications" to PermissionInfo(
        permission = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        rationale = "Notification permission is required to send you notifications."
    ),
    "Storage" to PermissionInfo(
        permission = arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
        rationale = "Storage permission is required to access your files."
    ),
    "Location" to PermissionInfo(
        permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        rationale = "Location permission is required to access your location."
    )
)