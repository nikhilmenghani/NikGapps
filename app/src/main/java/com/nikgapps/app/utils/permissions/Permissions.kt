package com.nikgapps.app.utils.permissions

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nikgapps.app.utils.constants.permissionMap

object Permissions {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permissionMap[permission]?.permission ?: ""
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionPermanentlyDenied(context: Context, permission: String): Boolean {
        return if (context is Activity) {
            !isPermissionGranted(context, permission) && !ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                permissionMap[permission]?.permission ?: ""
            )
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun requestPermission(
        context: Context,
        permissionName: String,
        onPermissionResult: (Boolean, Boolean) -> Unit
    ): ActivityResultLauncher<String>? {
        val permissionInfo = permissionMap[permissionName] ?: return null
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val permanentlyDenied = isPermissionPermanentlyDenied(context, permissionInfo.permission)
            onPermissionResult(isGranted, permanentlyDenied)
            val message = when {
                isGranted -> "$permissionName Permission Granted"
                permanentlyDenied -> "Denied Permanently, Go to Settings"
                else -> "$permissionName Permission Denied"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}