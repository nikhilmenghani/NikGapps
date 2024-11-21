package com.nikgapps.app.data.model

import androidx.compose.runtime.mutableStateListOf

object LogManager {
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    fun log(message: String) {
        _logs.add(message)
    }

    fun clearLogs() {
        _logs.clear()
    }
}

fun logProgress(message: String) {
    LogManager.log(message)
}