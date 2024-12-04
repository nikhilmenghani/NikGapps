package com.nikgapps.app.utils.root

import android.content.Context
import java.io.File

data class ScriptResult(val success: Boolean, val output: String)

class RootManager(private val context: Context? = null) {

    fun executeScript(scriptId: Int, vararg args: String, asRoot: Boolean = true): ScriptResult {
        val resourceName = context?.resources?.getResourceEntryName(scriptId)
        val scriptFile = File(context?.filesDir, "$resourceName.sh")
        val inputStream = context?.resources?.openRawResource(scriptId)
        return try {
            scriptFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            if (asRoot) {
                val process = Runtime.getRuntime().exec("su -c chmod +x $scriptFile.absolutePath")
                process.waitFor()
                executeScriptAsRoot(scriptFile.absolutePath, *args)
            } else {
                val process = Runtime.getRuntime().exec("chmod +x $scriptFile.absolutePath")
                process.waitFor()
                executeScript(scriptFile.absolutePath, *args)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ScriptResult(false, "Exception: ${e.message}")
        }
    }

    fun executeScript(filePath: String, vararg args: String): ScriptResult {
        return try {
            val process = Runtime.getRuntime().exec("su -c sh $filePath ${args.joinToString(" ")}")
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (error.isNotEmpty()) ScriptResult(false, "Error: $error") else ScriptResult(true, output)
        } catch (e: Exception) {
            e.printStackTrace()
            "Exception: ${e.message}"
            ScriptResult(false, "Exception: ${e.message}")
        }
    }

    fun executeScriptAsRoot(filePath: String, vararg args: String): ScriptResult {
        return try {
            val process = Runtime.getRuntime().exec("su -c sh $filePath ${args.joinToString(" ")}")
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (error.isNotEmpty()) ScriptResult(false, "Error: $error") else ScriptResult(true, output)
        } catch (e: Exception) {
            e.printStackTrace()
            "Exception: ${e.message}"
            ScriptResult(false, "Exception: ${e.message}")
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