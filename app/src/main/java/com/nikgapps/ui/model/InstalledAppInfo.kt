package com.nikgapps.ui.model

import androidx.compose.ui.graphics.painter.Painter

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val installLocation: String,
    val appIcon: Any,
    val isSystemApp: Boolean
)

