package com.nikgapps.app.utils.root

import android.content.Context
import com.nikgapps.app.utils.constants.ApplicationConstants.NIKGAPPS_APP_DIR
import java.io.File

class RootManager(private val context: Context? = null, private val runner: ShellRunner = ProcessShellRunner()) {
    fun executeScript(scriptId: Int, vararg args: String, asRoot: Boolean = true): ScriptResult = synchronized(RESOURCE_LOCK) {
        val owner = context ?: return@synchronized ScriptResult(false, "Context required")
        // Developer mount/test tools share only private flags, never externally writable scripts.
        val directory = File(owner.filesDir, "root-tools").apply { check(mkdirs() || isDirectory) }
        val script = File.createTempFile("root-script-", ".sh", directory)
        try {
            owner.resources.openRawResource(com.nikgapps.R.raw.variables).use { input ->
                File(directory, "variables.sh").outputStream().use { input.copyTo(it) }
            }
            owner.resources.openRawResource(scriptId).use { input -> script.outputStream().use { input.copyTo(it) } }
            ShellCommands.runScript(runner, script.absolutePath, args.toList(), asRoot)
        } finally { script.delete() }
    }
    fun executeScript(filePath: String, vararg args: String): ScriptResult =
        ShellCommands.runScript(runner, filePath, args.toList(), false)
    fun executeScriptAsRoot(filePath: String, vararg args: String): ScriptResult =
        ShellCommands.runScript(runner, filePath, args.toList(), true)
    fun executeCommandAsRoot(fileSource: String, command: String, vararg args: String): ScriptResult {
        require(command.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")))
        val source = if (fileSource.isEmpty()) "" else {
            val file = if (File(fileSource).isAbsolute) File(fileSource) else {
                require(fileSource.matches(Regex("[A-Za-z0-9_.-]+")) && fileSource != "..")
                File(NIKGAPPS_APP_DIR, fileSource)
            }
            ". ${ShellCommands.quote(file.absolutePath)} && "
        }
        return runner.run(listOf("su", "-c", "set -e; " + source + ShellCommands.command(command, args.toList())))
    }
    fun hasRootAccess(): Boolean = runner.run(listOf("su", "-c", "id -u")).let { it.success && it.output.trim() == "0" }
    companion object { private val RESOURCE_LOCK = Any() }
}
