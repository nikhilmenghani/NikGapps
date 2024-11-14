package com.nikgapps.app.utils.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat


object Notifications {

    @Composable
    fun getRequestPermissionLauncher(context: Context): ActivityResultLauncher<String> {
        // Define the launcher to request permissions
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(context, "Notifications Permission Granted", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Notifications Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }

    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Assume true for API levels below TIRAMISU as they don't require runtime notification permission
        }
    }
}
