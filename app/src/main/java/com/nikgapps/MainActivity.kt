package com.nikgapps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.navigation.ScreenNavigator
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.permissions.Permissions
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val progressLogViewModel: ProgressLogViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Permissions.hasAllRequiredPermissions(this)) {
            setContent {
                NikGappsTheme {
                    // Your composable content
                    ScreenNavigator(progressLogViewModel)
                }
            }
        } else {
            // Launch PermissionsActivity if any permissions are missing
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }

    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}