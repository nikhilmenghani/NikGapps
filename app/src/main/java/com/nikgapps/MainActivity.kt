package com.nikgapps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.navigation.AppNavigation
import com.nikgapps.ui.theme.NikGappsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            NikGappsTheme() {
//                // Your composable content
//                AppNavigation()
//            }
            MyApp(globalClass)
        }
    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}