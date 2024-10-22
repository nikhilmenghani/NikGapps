package com.nikgapps.ui.model

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val installLocation: String,
    val appIcon: Any,
    val isSystemApp: Boolean,
    val appType: String
)

