package com.nikgapps.app.data

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val installLocation: String,
    val appIcon: Any,
    val isSystemApp: Boolean,
    val appType: String
)

