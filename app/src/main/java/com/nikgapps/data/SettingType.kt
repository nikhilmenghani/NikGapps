package com.nikgapps.data

sealed class SettingType {
    object Toggle : SettingType()
    object Radio : SettingType()
    object Checkbox : SettingType()
    data class Text(val hint: String) : SettingType()
}