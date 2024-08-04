// SharedViewModel.kt
package com.nikgapps.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel(context: Context) : ViewModel() {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _useDynamicColor = MutableStateFlow(sharedPreferences.getBoolean("use_dynamic_color", true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun setUseDynamicColor(value: Boolean) {
        _useDynamicColor.value = value
        with(sharedPreferences.edit()) {
            putBoolean("use_dynamic_color", value)
            apply()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SharedViewModel(context)
            }
        }
    }
}
