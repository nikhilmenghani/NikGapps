package com.nikgapps.app.data.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object LogManager {
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    private const val APP_LOGS_FILE_NAME = "persistent_logs.txt"
    private const val SCRIPT_LOGS_FILE_PATH = "/data/local/tmp/mount_script.log"

    // Add a log entry and save it to the app's private storage
    fun log(message: String, context: Context) {
        _logs.add(message)
        saveAppLogs(context)
    }

    // Clear all logs and delete the persistent logs file
    fun clearLogs(context: Context) {
        _logs.clear()
        // Delete app-specific logs file
        val appLogsFile = File(context.filesDir, APP_LOGS_FILE_NAME)
        if (appLogsFile.exists()) {
            appLogsFile.delete()
        }

        // Truncate the script logs file
        val scriptLogsFile = File(SCRIPT_LOGS_FILE_PATH)
        if (scriptLogsFile.exists()) {
            scriptLogsFile.writeText("") // Clear the content of the script logs file
        }
    }

    // Load logs from both the app-specific logs file and the script's log file
    suspend fun loadLogs(context: Context) {
        withContext(Dispatchers.IO) {
            _logs.clear()

            // Load logs from the app-specific logs file
            val appLogsFile = File(context.filesDir, APP_LOGS_FILE_NAME)
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

            // Load logs from the script logs file
            val scriptLogsFile = File(SCRIPT_LOGS_FILE_PATH)
            if (scriptLogsFile.exists()) {
                try {
                    FileInputStream(scriptLogsFile).use { fis ->
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
    private fun saveAppLogs(context: Context) {
        val file = File(context.filesDir, APP_LOGS_FILE_NAME)
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
