package com.nikgapps.app.utils.root

import android.content.Context
import java.io.File

data class ScriptResult(val success: Boolean, val output: String)

class RootManager(private val context: Context) {

    fun executeScript(scriptId: Int, vararg args: String): ScriptResult {
        val resourceName = context.resources.getResourceEntryName(scriptId)
        val scriptFile = File(context.filesDir, "$resourceName.sh")
        val inputStream = context.resources.openRawResource(scriptId)
        return try {
            scriptFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            val process = Runtime.getRuntime().exec("su -c chmod +x $scriptFile.absolutePath")
            process.waitFor()
            val result = executeScript(scriptFile.absolutePath, *args)
            ScriptResult(true, result)
        } catch (e: Exception) {
            e.printStackTrace()
            ScriptResult(false, "Exception: ${e.message}")
        }
    }

    fun executeScript(filePath: String, vararg args: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c sh $filePath ${args.joinToString(" ")}")
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (error.isNotEmpty()) "Error: $error" else output
        } catch (e: Exception) {
            e.printStackTrace()
            "Exception: ${e.message}"
        }
    }

    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}