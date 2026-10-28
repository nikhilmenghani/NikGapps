package com.nikgapps.app.utils.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nikgapps.app.utils.application.ApplicationMode
import com.nikgapps.app.utils.constants.PermissionConstants
import com.nikgapps.app.utils.constants.PermissionConstants.permissionMap

object Permissions {

    private const val PREFS_NAME = "permission_prefs"
    private const val KEY_PERMISSION_REQUESTED = "requested_"

    fun hasAllRequiredPermissions(context: Context): Boolean {
//        if (ApplicationMode.isInPreviewMode()) return true // Allow preview mode to skip permissions)
        permissionMap.forEach { (permissionName) ->
            if (!isPermissionGranted(context, permissionName)) {
                return false
            }
        }
        return true
    }

    fun isPermissionGranted(context: Context, permissionName: String): Boolean {
//        if (ApplicationMode.isInPreviewMode()) return true // Allow preview mode to skip permissions
        val permissions = permissionMap[permissionName]?.permission ?: return false
        return when (permissionName) {
            PermissionConstants.INSTALL_APPS -> {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
            }
            PermissionConstants.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    permissions.all { permission ->
                        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
            }
            else -> {
                permissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    fun isPermissionPermanentlyDenied(context: Context, permissionName: String): Boolean {
//        if (ApplicationMode.isInPreviewMode()) return false // Allow preview mode to skip permissions
        val permissions = permissionMap[permissionName]?.permission ?: return false
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return if (context is Activity) {
            permanentlyDeniedPermissions(permissions.toList(),
                wasRequested = { sharedPreferences.getBoolean(KEY_PERMISSION_REQUESTED + it, false) },
                isGranted = { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED },
                shouldShowRationale = { ActivityCompat.shouldShowRequestPermissionRationale(context, it) })
        } else {
            false
        }
    }

    fun markPermissionAsRequested(context: Context, permissionName: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            permissionMap[permissionName]?.permission?.forEach { permission ->
                putBoolean(KEY_PERMISSION_REQUESTED + permission, true)
            }
        }.apply()
    }

    @Composable
    fun requestPermission(
        context: Context,
        permissionName: String,
        onPermissionResult: (Boolean, Boolean) -> Unit
    ): ActivityResultLauncher<Array<String>>? {
        permissionMap[permissionName] ?: return null
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // Re-read the entire group: one grant must not hide another denied permission.
            val isGranted = isPermissionGranted(context, permissionName)
            val permanentlyDenied = isPermissionPermanentlyDenied(context, permissionName)
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
