package com.nikgapps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.screen.PermissionsScreen

class PermissionsActivity: ComponentActivity() {
    companion object {
        const val EXTRA_REVIEW_MODE = "com.nikgapps.extra.REVIEW_PERMISSIONS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reviewMode = intent.getBooleanExtra(EXTRA_REVIEW_MODE, false)
        setContent {
            NikGappsTheme {
                PermissionsScreen(
                    autoCompleteWhenGranted = !reviewMode,
                    onAllPermissionsGranted = ::onAllPermissionsGranted,
                    onBack = if (reviewMode) ::finish else null
                )
            }
        }
    }

    private fun onAllPermissionsGranted() {
        if (Permissions.hasAllRequiredPermissions(this)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
