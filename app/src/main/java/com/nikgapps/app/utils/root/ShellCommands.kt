package com.nikgapps.app.utils.root

data class ScriptResult(val success: Boolean, val output: String)
fun interface ShellRunner { fun run(arguments: List<String>): ScriptResult }

object ShellCommands {
    fun quote(value: String): String {
        require('\u0000' !in value) { "Shell arguments cannot contain NUL" }
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }
    fun command(command: String, arguments: List<String>): String {
        require(command.matches(Regex("[A-Za-z_][A-Za-z0-9_./-]*"))) { "Invalid command" }
        return (listOf(quote(command)) + arguments.map(::quote)).joinToString(" ")
    }
    fun runScript(runner: ShellRunner, path: String, args: List<String>, root: Boolean): ScriptResult =
        runner.run(if (root) listOf("su", "-c", command("sh", listOf("-e", path) + args))
            else listOf("sh", "-e", path) + args)
}

class ProcessShellRunner : ShellRunner {
    override fun run(arguments: List<String>): ScriptResult {
        var process: Process? = null
        var watchdog: Thread? = null
        return try {
            process = ProcessBuilder(arguments).redirectErrorStream(true).start()
            val child = process
            val owner = Thread.currentThread()
            watchdog = Thread({
                try {
                    val deadline = System.nanoTime() + 120_000_000_000L
                    while (System.nanoTime() < deadline && !owner.isInterrupted) Thread.sleep(100)
                    child.destroy()
                } catch (_: InterruptedException) { /* Completed normally. */ }
            }, "root-command-watchdog").apply { isDaemon = true; start() }
            val bytes = java.io.ByteArrayOutputStream()
            process.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    val retained = minOf(count, 1024 * 1024 - bytes.size())
                    if (retained > 0) bytes.write(buffer, 0, retained)
                }
            }
            val output = bytes.toString("UTF-8")
            if (owner.isInterrupted) throw InterruptedException("Shell command cancelled")
            ScriptResult(process.waitFor() == 0, output)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt(); throw interrupted
        } catch (error: Exception) { ScriptResult(false, error.message.orEmpty()) }
        finally { watchdog?.interrupt(); process?.destroy() }
    }
}
