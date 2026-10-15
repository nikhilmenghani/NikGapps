package com.nikgapps.app.utils

import com.nikgapps.app.utils.root.*
import org.junit.Assert.*
import org.junit.Test

class ShellCommandsTest {
    @Test fun fakeRunnerReceivesOnlyLiteralArguments() {
        val values = listOf("a b", "'\"", "a\nb", "\$(touch /bad)", "`id`", "; & | > < *")
        var captured = emptyList<String>()
        val fake = ShellRunner { captured = it; ScriptResult(false, "failed") }
        assertFalse(ShellCommands.runScript(fake, "/path with space/file", values, true).success)
        assertEquals(listOf("su", "-c", ShellCommands.command("sh", listOf("-e", "/path with space/file") + values)), captured)
        ShellCommands.runScript(fake, "/path", values, false)
        assertEquals(listOf("sh", "-e", "/path") + values, captured)
        assertEquals("'a'\"'\"'b'", ShellCommands.quote("a'b"))
        assertThrows(IllegalArgumentException::class.java) { ShellCommands.quote("bad\u0000") }
        assertThrows(IllegalArgumentException::class.java) { ShellCommands.command("id;reboot", emptyList()) }
    }
    @Test fun metadataCannotExecuteDuringScriptGeneration() {
        val pkg = com.nikgapps.app.data.Package(packageTitle = "Clock", partition = "product", packageName = "\$(evil)\n'", fileList = listOf("___app___Clock/Clock.apk"))
        val script = pkg.getInstallScript("trusted")
        assertTrue(script.contains("pkg_name=" + ShellCommands.quote(pkg.packageName)))
        assertThrows(IllegalArgumentException::class.java) { pkg.copy(partition = "product;id").getInstallScript("trusted") }
        assertThrows(IllegalArgumentException::class.java) { pkg.copy(packageTitle = "../escape").getInstallScript("trusted") }
        assertThrows(IllegalArgumentException::class.java) { pkg.copy(fileList = listOf("___product___..___..___data/escape.apk")).getInstallScript("trusted") }
    }
}
