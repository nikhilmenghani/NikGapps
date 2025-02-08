package com.nikgapps.app.data

import androidx.compose.runtime.mutableStateListOf
import com.nikgapps.app.utils.constants.ApplicationConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object LogManager {
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    val APP_LOGS_FILE_NAME = "${ApplicationConstants.getNikGappsDirectory()}/NikGapps_logs.log"

    // Add a log entry and save it to the app's private storage
    fun log(message: String) {
        _logs.add(message)
        saveAppLogs()
    }

    // Clear all logs and delete the persistent logs file
    fun clearLogs() {
        _logs.clear()
        // Delete app-specific logs file
        val appLogsFile = File(APP_LOGS_FILE_NAME)
        if (appLogsFile.exists()) {
            appLogsFile.delete()
        }
    }

    // Load logs from both the app-specific logs file and the script's log file
    suspend fun loadLogs() {
        withContext(Dispatchers.IO) {
            _logs.clear()

            // Load logs from the app-specific logs file
            val appLogsFile = File(APP_LOGS_FILE_NAME)
            if (appLogsFile.exists()) {
                try {
                    FileInputStream(appLogsFile).use { fis ->
                        fis.bufferedReader().forEachLine { line ->
                            _logs.add(line)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Save app-specific logs to the app's private storage
    private fun saveAppLogs() {
        val file = File(APP_LOGS_FILE_NAME)
        try {
            FileOutputStream(file).use { fos ->
                _logs.forEach { log ->
                    fos.write((log + "\n").toByteArray())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
