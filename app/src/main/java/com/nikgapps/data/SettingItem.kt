package com.nikgapps.data

data class SettingItem(
    val key: String,
    val title: String,
    val type: SettingType,
    val value: Any,
    val category: String, // Category to which the setting belongs
    val isStateFlow: Boolean = false, // Indicates if this setting should be managed as a StateFlow
    val visibilityCondition: ((Map<String, Any>) -> Boolean)? = null, // Condition for dynamic visibility
    val textToDisplay: String // Text to display in the UI
)