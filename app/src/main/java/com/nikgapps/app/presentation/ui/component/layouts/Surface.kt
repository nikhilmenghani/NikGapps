package com.nikgapps.app.presentation.ui.component.layouts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.nikgapps.app.presentation.ui.component.common.Greeting

@Composable
fun SurfaceContainer(){
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        Greeting("Hello World")
    }
}