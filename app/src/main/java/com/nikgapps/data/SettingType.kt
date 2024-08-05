package com.nikgapps.data

sealed class SettingType {
    object Toggle : SettingType()
    class Radio(val options: List<String>) : SettingType() // Define as class to include options
    object Checkbox : SettingType()
    data class Text(val hint: String) : SettingType()
    object Dialog : SettingType() // New type for dialog
}