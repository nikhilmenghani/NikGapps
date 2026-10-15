package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.*
import kotlinx.coroutines.CancellationException

class SafeFilePublicationTest {
    @Test fun concurrentPublicationsCannotMixBytesOrDeleteAnotherStage() {
        val dir = Files.createTempDirectory("concurrent-publication").toFile()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val destination = File(dir, "shared.zip")
            val tasks = listOf("a", "b").map { value -> executor.submit {
                SafeFilePublication.write(destination, { value.repeat(100000).byteInputStream() }, {
                    require(it.readText() == value.repeat(100000))
                })
            } }
            tasks.forEach { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            assertTrue(destination.readText() in listOf("a".repeat(100000), "b".repeat(100000)))
            assertEquals(listOf("shared.zip"), dir.list()!!.toList())
        } finally { executor.shutdownNow(); dir.deleteRecursively() }
    }
    @Test fun failureCancellationReplacementAndRecoveryPreserveOldOutput() {
        val dir = Files.createTempDirectory("publication").toFile()
        try {
            val target = File(dir, "out.zip").apply { writeText("old") }
            fun attempt(input: () -> InputStream, checkpoint: () -> Unit = {}, rename: (File, File) -> Boolean = { a,b -> a.renameTo(b) }) {
                try { SafeFilePublication.write(target, input, { require(it.readText() == "new") }, checkpoint, rename); fail() }
                catch (_: Exception) { assertEquals("old", target.readText()) }
            }
            attempt({ object : InputStream() { override fun read(): Int = throw IOException("ENOSPC") } })
            attempt({ "bad".byteInputStream() })
            attempt({ "new".byteInputStream() }, { throw CancellationException() })
            attempt({ "new".byteInputStream() }, rename = { a,b -> if (a.name.endsWith(".part")) false else a.renameTo(b) })
            assertTrue(target.renameTo(File(dir, "out.zip.backup")))
            SafeFilePublication.write(target, { "new".byteInputStream() }, { require(it.readText() == "new") })
            assertEquals("new", target.readText())
            assertFalse(dir.listFiles()!!.any { it.name.endsWith(".part") || it.name.endsWith(".backup") })
        } finally { dir.deleteRecursively() }
    }
}
