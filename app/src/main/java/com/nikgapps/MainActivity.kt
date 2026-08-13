package com.nikgapps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.navigation.ScreenNavigator
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.security.AppLock
import com.nikgapps.app.security.canUseAppLock
import com.nikgapps.app.update.AppUpdateManager
import com.nikgapps.app.utils.AppDiagnostics
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val progressLogViewModel: ProgressLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.info("app", "launched", mapOf(
            "version" to BuildConfig.VERSION_NAME,
            "androidApi" to Build.VERSION.SDK_INT
        ))
        if (Permissions.hasAllRequiredPermissions(this)) {
            setContent {
                NikGappsTheme {
                    AppLock(
                        enabled = globalClass.preferencesManager.displayPrefs.biometricLockEnabled && canUseAppLock(this@MainActivity),
                        activity = this@MainActivity
                    ) { ScreenNavigator(progressLogViewModel) }
                }
            }
        } else {
            // Launch PermissionsActivity if any permissions are missing
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdateManager.checkOnAppStart(this)
    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
