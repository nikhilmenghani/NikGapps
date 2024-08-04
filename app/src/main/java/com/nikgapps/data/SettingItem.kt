package com.nikgapps.data

data class SettingItem(
    val key: String,
    val title: String,
    val type: SettingType,
    val value: Any,
    val isStateFlow: Boolean = false // Indicates if this setting should be managed as a StateFlow
)