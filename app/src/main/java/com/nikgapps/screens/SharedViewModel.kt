// SharedViewModel.kt
package com.nikgapps.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nikgapps.data.PreferencesManager
import com.nikgapps.data.SettingItem
import com.nikgapps.data.SettingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {
    private val _settings = MutableStateFlow(preferencesManager.getAllSettings())
    val settings: StateFlow<List<SettingItem>> = _settings.asStateFlow()

    fun updateSetting(item: SettingItem) {
        preferencesManager.setSettingItem(item)
        _settings.value = preferencesManager.getAllSettings() // Update the StateFlow
    }

    fun getSettingValue(key: String, type: SettingType): Any {
        return preferencesManager.getSettingValue(key, type)
    }

    fun getSettingStateFlow(key: String): StateFlow<Any>? {
        return preferencesManager.getStateFlow(key)
    }

    fun setSettingValue(key: String, value: Any, type: SettingType) {
        preferencesManager.setSettingValue(key, value, type)
        _settings.value = preferencesManager.getAllSettings() // Update the StateFlow
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SharedViewModel(PreferencesManager(context))
            }
        }
    }
}
