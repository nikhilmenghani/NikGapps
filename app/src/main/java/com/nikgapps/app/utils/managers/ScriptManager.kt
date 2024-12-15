package com.nikgapps.app.utils.managers

import java.io.File

object ScriptManager {
    fun createScriptFile(fileName: String, content: String): File {
        val scriptFile = File(fileName)
        if (scriptFile.parentFile?.exists() == false) {
            scriptFile.parentFile?.mkdirs()
        }
        scriptFile.writeText(content)
        scriptFile.setExecutable(true)
        return scriptFile
    }
}