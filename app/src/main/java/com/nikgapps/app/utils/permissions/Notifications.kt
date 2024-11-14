package com.nikgapps.app.utils.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Notifications {
    // Check if notification permission is granted
    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Assume granted for versions below TIRAMISU
        }
    }

    // Check if notification permission is permanently denied
    fun isPermissionPermanentlyDenied(context: Context): Boolean {
        return if (context is Activity) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                false
            } else {
                !isPermissionGranted(context) && !ActivityCompat.shouldShowRequestPermissionRationale(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            false
        }
    }

    // Launch permission request with callback
    @Composable
    fun requestPermission(
        context: Context,
        onPermissionResult: (Boolean, Boolean) -> Unit
    ): ActivityResultLauncher<String> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val permanentlyDenied = isPermissionPermanentlyDenied(context)
            onPermissionResult(isGranted, permanentlyDenied)
            val message = when {
                isGranted -> "Notifications Permission Granted"
                permanentlyDenied -> "Denied Permanently, Go to Settings"
                else -> "Notifications Permission Denied"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
