package com.nikgapps.app.data.model

import androidx.compose.runtime.mutableStateListOf

object ProgressLogManager {
    private val _progressLogs = mutableStateListOf<String>()
    val progressLogs: List<String> get() = _progressLogs

    fun progressLog(message: String) {
        _progressLogs.add(message)
    }

    fun clearLogs() {
        _progressLogs.clear()
    }
}