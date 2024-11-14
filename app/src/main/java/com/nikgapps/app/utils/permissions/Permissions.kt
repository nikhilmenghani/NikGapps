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
    fun isPermissionGranted(context: Context, permissionName: String): Boolean {
        val permissions = permissionMap[permissionName]?.permission ?: return false
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionPermanentlyDenied(context: Context, permissionName: String): Boolean {
        val permissions = permissionMap[permissionName]?.permission ?: return false
        return if (context is Activity) {
            permissions.any { permission ->
                !isPermissionGranted(context, permissionName) && !ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
            }
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
            val permissions = permissionInfo.permission
            val permanentlyDenied = permissions.any { permission ->
                isPermissionPermanentlyDenied(context, permission)
            }
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