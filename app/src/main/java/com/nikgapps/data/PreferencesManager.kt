// PreferencesManager.kt
package com.nikgapps.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val stateFlows = mutableMapOf<String, MutableStateFlow<Any>>()

    init {
        // Initialize StateFlows for settings that should use StateFlow
        getAllSettings().filter { it.isStateFlow }.forEach {
            stateFlows[it.key] = MutableStateFlow(getSettingValue(it.key, it.type))
        }
    }

    fun getSettingItem(key: String, type: SettingType): SettingItem {
        val value: Any = when (type) {
            is SettingType.Toggle -> sharedPreferences.getBoolean(key, false)
            is SettingType.Radio -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Checkbox -> sharedPreferences.getBoolean(key, false)
            is SettingType.Text -> sharedPreferences.getString(key, "") ?: ""
        }
        return SettingItem(key, key.capitalize(), type, value, isStateFlow = stateFlows.containsKey(key))
    }

    fun setSettingItem(item: SettingItem) {
        with(sharedPreferences.edit()) {
            when (item.type) {
                is SettingType.Toggle -> putBoolean(item.key, item.value as Boolean)
                is SettingType.Radio -> putString(item.key, item.value as String)
                is SettingType.Checkbox -> putBoolean(item.key, item.value as Boolean)
                is SettingType.Text -> putString(item.key, item.value as String)
            }
            apply()
        }
        stateFlows[item.key]?.value = item.value
    }

    fun getAllSettings(): List<SettingItem> {
        return listOf(
            getSettingItem("use_dynamic_color", SettingType.Toggle).copy(isStateFlow = true),
            getSettingItem("another_setting", SettingType.Text("Enter value"))
            // Add more settings as needed
        )
    }

    fun getSettingValue(key: String, type: SettingType): Any {
        return when (type) {
            is SettingType.Toggle -> sharedPreferences.getBoolean(key, false)
            is SettingType.Radio -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Checkbox -> sharedPreferences.getBoolean(key, false)
            is SettingType.Text -> sharedPreferences.getString(key, "") ?: ""
        }
    }

    fun setSettingValue(key: String, value: Any, type: SettingType) {
        with(sharedPreferences.edit()) {
            when (type) {
                is SettingType.Toggle -> putBoolean(key, value as Boolean)
                is SettingType.Radio -> putString(key, value as String)
                is SettingType.Checkbox -> putBoolean(key, value as Boolean)
                is SettingType.Text -> putString(key, value as String)
            }
            apply()
        }
        stateFlows[key]?.value = value
    }

    fun getStateFlow(key: String): StateFlow<Any>? {
        return stateFlows[key]?.asStateFlow()
    }
}
