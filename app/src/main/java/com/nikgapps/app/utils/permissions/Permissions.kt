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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nikgapps.app.utils.constants.permissionMap

object Permissions {

    private const val PREFS_NAME = "permission_prefs"
    private const val KEY_PERMISSION_REQUESTED = "requested_"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun hasAllRequiredPermissions(context: Context): Boolean {
        permissionMap.forEach { (permissionName) ->
            if (!isPermissionGranted(context, permissionName)) {
                return false
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionGranted(context: Context, permissionName: String): Boolean {
        val permissions = permissionMap[permissionName]?.permission ?: return false
        return when (permissionName) {
            "Storage" -> {
                Environment.isExternalStorageManager()
            }
            else -> {
                permissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isPermissionPermanentlyDenied(context: Context, permissionName: String): Boolean {
        val permissions = permissionMap[permissionName]?.permission ?: return false
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if this permission has been requested before
        val permissionRequested = sharedPreferences.getBoolean(KEY_PERMISSION_REQUESTED + permissionName, false)

        return if (context is Activity) {
            permissions.any { permission ->
                val isDenied = ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(context, permission)

                // If this permission has been requested and rationale shouldn't show, consider it permanently denied
                isDenied && permissionRequested && !shouldShowRationale
            }
        } else {
            false
        }
    }

    fun markPermissionAsRequested(context: Context, permissionName: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_PERMISSION_REQUESTED + permissionName, true).apply()
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