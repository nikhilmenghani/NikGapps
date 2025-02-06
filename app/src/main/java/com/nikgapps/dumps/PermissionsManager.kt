package com.nikgapps.dumps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nikgapps.App
import com.nikgapps.R

abstract class PermissionsManager : ComponentActivity() {

    private val checkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (canAccessStorage()) {
                onPermissionGranted()
            } else {
                App.Companion.globalClass.showMsg(R.string.storage_permission_required)
                finish()
            }
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                onPermissionGranted()
            } else {
                App.Companion.globalClass.showMsg(R.string.storage_permission_required)
                finish()
            }
        }

    open fun onPermissionGranted() {}

    private fun canAccessStorage(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        return Environment.isExternalStorageManager()
    }

    private fun grantStoragePermissions(): Boolean {
        if (canAccessStorage()) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.fromParts("package", packageName, null)
            checkPermissionLauncher.launch(intent)
        } else {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
        return false
    }
}