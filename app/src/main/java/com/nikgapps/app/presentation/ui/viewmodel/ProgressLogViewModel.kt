package com.nikgapps.app.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProgressLogViewModel : ViewModel() {
    private val _progressLogs = MutableStateFlow<List<String>>(emptyList())
    val progressLogs: StateFlow<List<String>> get() = _progressLogs

    fun addLog(message: String) {
        _progressLogs.value += message
    }

    fun clearLogs() {
        _progressLogs.value = emptyList()
    }
}