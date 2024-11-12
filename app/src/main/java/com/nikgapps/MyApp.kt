package com.nikgapps

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.nikgapps.ui.screens.apps.AppsScreen
import com.nikgapps.app.presentation.theme.NikGappsTheme

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MyApp() {
    NikGappsTheme {
        AppsScreen()
    }
}
