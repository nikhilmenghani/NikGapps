package com.nikgapps.app.registry

import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile

/** Same-directory staging; an interrupted fallback leaves a recoverable .backup file. */
object SafeFilePublication {
    private val lock = Any()
    fun validZip(file: File, checkpoint: () -> Unit = {}) {
        ZipFile(file).use { zip ->
            require(zip.size() in 1..20000) { "Empty ZIP or too many entries" }
            var total = 0L
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement(); if (entry.isDirectory) continue
                val crc = java.util.zip.CRC32(); var size = 0L
                zip.getInputStream(entry).use { input ->
                    val bytes = ByteArray(32768)
                    while (true) { checkpoint(); val count = input.read(bytes); if (count < 0) break
                        crc.update(bytes, 0, count); size += count; total += count
                        require(total <= 8L * 1024 * 1024 * 1024) { "ZIP expanded size exceeds limit" } }
                }
                require(size == entry.size && crc.value == entry.crc) { "Corrupt ZIP" }
            }
        }
    }
    fun write(destination: File, input: () -> InputStream, validate: (File) -> Unit,
        checkpoint: () -> Unit = {}, rename: (File, File) -> Boolean = { a, b -> a.renameTo(b) }): File = synchronized(lock) {
        require(destination.parentFile!!.mkdirs() || destination.parentFile!!.isDirectory)
        require(destination.canonicalFile.parentFile == destination.parentFile!!.canonicalFile) { "Unsafe output link" }
        val backup = File(destination.parentFile, destination.name + ".backup")
        require(backup.canonicalFile.parentFile == destination.parentFile!!.canonicalFile) { "Unsafe backup link" }
        // Recover a crash between backup and promotion, before doing any new writes.
        if (backup.exists() && !destination.exists()) check(rename(backup, destination)) { "Cannot recover previous output" }
        if (backup.exists()) { validate(destination); check(backup.delete()) }
        val stage = File(destination.parentFile, ".${destination.name}.${UUID.randomUUID()}.part")
        try {
            input().use { source -> java.io.FileOutputStream(stage).use { output ->
                val bytes = ByteArray(32768)
                while (true) { checkpoint(); val count = source.read(bytes); if (count < 0) break; output.write(bytes, 0, count) }
                output.fd.sync()
            } }
            validate(stage); checkpoint()
            if (destination.exists()) check(rename(destination, backup)) { "Cannot back up previous output" }
            try { check(rename(stage, destination)) { "Cannot promote staged output" } }
            catch (error: Throwable) {
                if (backup.exists()) check(rename(backup, destination)) { "Previous output preserved at ${backup.path}" }
                throw error
            }
            // No cancellation points between rename and successful return.
            backup.delete()
            destination
        } finally { stage.delete() }
    }
}
