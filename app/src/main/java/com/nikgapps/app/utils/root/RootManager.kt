package com.nikgapps.app.utils.root

import android.content.Context
import java.io.File

data class ScriptResult(val success: Boolean, val output: String)

class RootManager(private val context: Context) {

    var rootAccess: Boolean = false
    var command: String = ""
    var sourcePath: String = ""
    var destPath: String = ""

    fun executeScript(scriptId: Int, vararg args: String): ScriptResult {
        val resourceName = context.resources.getResourceEntryName(scriptId)
        val scriptFile = File(context.filesDir, "$resourceName.sh")
        val inputStream = context.resources.openRawResource(scriptId)
        val scriptPath = scriptFile.absolutePath
        return try {
            scriptFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            val process = Runtime.getRuntime().exec("su -c chmod +x $scriptPath")
            process.waitFor()
            val result = executeScript(scriptPath, *args)
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
}