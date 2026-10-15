package com.nikgapps.app.utils

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.*

class SafeZipExtractorTest {
    private fun zip(file: File, name: String, bytes: ByteArray = "payload".toByteArray()): File = file.apply {
        ZipOutputStream(outputStream()).use { out -> out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry() }
    }
    @Test fun pathsAndNestedTraversalAreRejected() = runBlocking {
        val dir = Files.createTempDirectory("safe-zip").toFile()
        try {
            listOf("../escape", "/escape", "C:/escape", "a/../../escape", "a\\escape").forEachIndexed { i, name ->
                try { SafeZipExtractor.extract(zip(File(dir, "$i.zip"), name), File(dir, "out$i")); fail(name) }
                catch (_: IllegalArgumentException) { assertFalse(File(dir, "out$i").exists()) }
            }
            val inner = zip(File(dir, "inner.zip"), "../escape")
            val outer = zip(File(dir, "outer.zip"), "nested.zip", inner.readBytes())
            try { SafeZipExtractor.extract(outer, File(dir, "nested"), true); fail() }
            catch (_: IllegalArgumentException) { assertFalse(File(dir, "nested").exists()) }
            val safeInner = zip(File(dir, "safe-inner.zip"), "safe")
            val deep = zip(File(dir, "deep.zip"), "nested.zip", safeInner.readBytes())
            try { SafeZipExtractor.extract(deep, File(dir, "deep"), true, ExtractionLimits(depth = 0)); fail() }
            catch (_: IllegalArgumentException) { assertFalse(File(dir, "deep").exists()) }
        } finally { dir.deleteRecursively() }
    }
    @Test fun malformedLimitsCancellationAndLinks() = runBlocking {
        val dir = Files.createTempDirectory("safe-zip-limits").toFile()
        try {
            val archive = zip(File(dir, "good.zip"), "a")
            SafeZipExtractor.extract(archive, File(dir, "valid"))
            assertEquals("payload", File(dir, "valid/a").readText())
            for (limit in listOf(ExtractionLimits(bytes = 1), ExtractionLimits(entries = 0))) {
                try { SafeZipExtractor.extract(archive, File(dir, "limited"), limits = limit); fail() }
                catch (_: IllegalArgumentException) { assertFalse(File(dir, "limited").exists()) }
            }
            try { SafeZipExtractor.extract(archive, File(dir, "cancel"), progress = { throw CancellationException() }); fail() }
            catch (_: CancellationException) { assertFalse(File(dir, "cancel").exists()) }
            val bad = File(dir, "bad.zip").apply { writeText("not a zip") }
            try { SafeZipExtractor.extract(bad, File(dir, "bad")); fail() } catch (_: Exception) { assertFalse(File(dir, "bad").exists()) }
            val data = archive.readBytes()
            val header = data.indices.first { it + 3 < data.size && data[it] == 0x50.toByte() && data[it+1] == 0x4b.toByte() && data[it+2] == 1.toByte() && data[it+3] == 2.toByte() }
            data[header + 40] = 0xff.toByte(); data[header + 41] = 0xa1.toByte()
            bad.writeBytes(data)
            try { SafeZipExtractor.extract(bad, File(dir, "link")); fail() } catch (_: IllegalArgumentException) { assertFalse(File(dir, "link").exists()) }
        } finally { dir.deleteRecursively() }
    }
}
