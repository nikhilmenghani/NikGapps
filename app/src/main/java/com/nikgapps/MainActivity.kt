package com.nikgapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nikgapps.navigation.AppNavigation
import com.nikgapps.ui.theme.NikGappsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NikGappsTheme {
                AppNavigation()
            }
        }
    }
}