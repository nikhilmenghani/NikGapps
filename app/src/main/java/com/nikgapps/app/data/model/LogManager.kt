package com.nikgapps.app.data.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object LogManager {
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    private const val LOGS_FILE_NAME = "persistent_logs.txt"

    fun log(message: String, context: Context) {
        _logs.add(message)
        saveLogs(context)
    }

    fun clearLogs(context: Context) {
        _logs.clear()
        val file = File(context.filesDir, LOGS_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    fun loadLogs(context: Context) {
        val file = File(context.filesDir, LOGS_FILE_NAME)
        if (file.exists()) {
            try {
                FileInputStream(file).use { fis ->
                    _logs.clear()
                    fis.bufferedReader().forEachLine { line ->
                        _logs.add(line)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveLogs(context: Context) {
        val file = File(context.filesDir, LOGS_FILE_NAME)
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