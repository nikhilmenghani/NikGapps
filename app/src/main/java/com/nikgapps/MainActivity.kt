package com.nikgapps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.nikgapps.navigation.AppNavigation
import com.nikgapps.app.presentation.theme.NikGappsTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NikGappsTheme() {
                // Your composable content
                AppNavigation()
            }
        }
    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}