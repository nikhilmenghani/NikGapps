// PreferencesManager.kt
package com.nikgapps.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val stateFlows = mutableMapOf<String, MutableStateFlow<Any>>()

    init {
        // Initialize StateFlows for settings that should use StateFlow
        getAllSettings().filter { it.isStateFlow }.forEach {
            stateFlows[it.key] = MutableStateFlow(getSettingValue(it.key, it.type))
        }
    }

    fun getSettingItem(
        key: String,
        type: SettingType,
        category: String,
        visibilityCondition: ((Map<String, Any>) -> Boolean)? = null,
        textToDisplay: String
    ): SettingItem {
        val value: Any = when (type) {
            is SettingType.Toggle -> sharedPreferences.getBoolean(key, false)
            is SettingType.Radio -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Checkbox -> sharedPreferences.getBoolean(key, false)
            is SettingType.Text -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Dialog -> sharedPreferences.getString(key, "") ?: "" // Default value for Dialog
        }
        return SettingItem(key,
            key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, type, value, category, isStateFlow = stateFlows.containsKey(key), visibilityCondition, textToDisplay)
    }

    fun setSettingItem(item: SettingItem) {
        with(sharedPreferences.edit()) {
            when (item.type) {
                is SettingType.Toggle -> putBoolean(item.key, item.value as Boolean)
                is SettingType.Radio -> putString(item.key, item.value as String)
                is SettingType.Checkbox -> putBoolean(item.key, item.value as Boolean)
                is SettingType.Text -> putString(item.key, item.value as String)
                is SettingType.Dialog -> putString(item.key, item.value as String)
            }
            apply()
        }
        stateFlows[item.key]?.value = item.value
    }

    fun getAllSettings(): List<SettingItem> {
        return listOf(
            getSettingItem("use_dynamic_color", SettingType.Toggle, "UI", null, "Use Dynamic Color").copy(isStateFlow = true),
            getSettingItem("another_setting", SettingType.Text("Enter value"), "General", null, "Another Setting"),
            getSettingItem("enable_feature", SettingType.Checkbox, "Features", null, "Enable Feature"),
            getSettingItem("theme_choice", SettingType.Radio(listOf("Light", "Dark", "System")), "UI", null, "Theme Choice"),
            getSettingItem("choose_account", SettingType.Dialog, "Account", null, "Choose Account") // New setting item
        )
    }

    fun getSettingValue(key: String, type: SettingType): Any {
        return when (type) {
            is SettingType.Toggle -> sharedPreferences.getBoolean(key, false)
            is SettingType.Radio -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Checkbox -> sharedPreferences.getBoolean(key, false)
            is SettingType.Text -> sharedPreferences.getString(key, "") ?: ""
            is SettingType.Dialog -> sharedPreferences.getString(key, "") ?: ""
        }
    }

    fun setSettingValue(key: String, value: Any, type: SettingType) {
        with(sharedPreferences.edit()) {
            when (type) {
                is SettingType.Toggle -> putBoolean(key, value as Boolean)
                is SettingType.Radio -> putString(key, value as String)
                is SettingType.Checkbox -> putBoolean(key, value as Boolean)
                is SettingType.Text -> putString(key, value as String)
                is SettingType.Dialog -> putString(key, value as String)
            }
            apply()
        }
        stateFlows[key]?.value = value
    }

    fun getStateFlow(key: String): StateFlow<Any>? {
        return stateFlows[key]?.asStateFlow()
    }
}
