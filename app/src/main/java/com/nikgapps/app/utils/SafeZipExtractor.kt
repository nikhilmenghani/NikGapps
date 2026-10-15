package com.nikgapps.app.utils

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class ExtractionLimits(val bytes: Long = 4L * 1024 * 1024 * 1024, val entries: Int = 20000, val depth: Int = 3)

/** Never follows archive links. Callers must supply an operation-private, non-existing destination. */
object SafeZipExtractor {
    fun contained(root: File, name: String): File {
        require(name.isNotBlank() && !name.startsWith('/') && !name.startsWith('\\') &&
            !name.contains('\\') && !name.contains(':') && !name.contains('\u0000') &&
            name.split('/').none { it == ".." || it == "." }) { "Unsafe ZIP path" }
        val base = root.absoluteFile
        val target = File(base, name).absoluteFile
        require(target.canonicalPath == target.path && target.path.startsWith(base.canonicalPath + File.separator)) {
            "ZIP entry escapes destination or follows a link"
        }
        return target
    }

    // java.util.zip does not expose Unix file types. Inspect the central directory before writing.
    private fun inspect(file: File, maxEntries: Int) {
        RandomAccessFile(file, "r").use { input ->
            fun u16(): Int = input.readUnsignedByte() or (input.readUnsignedByte() shl 8)
            fun u32(): Long = u16().toLong() or (u16().toLong() shl 16)
            var end = input.length() - 22
            val floor = maxOf(0L, input.length() - 65557)
            while (end >= floor) {
                input.seek(end)
                if (u32() == 0x06054b50L) {
                    input.seek(end + 20)
                    if (end + 22 + u16() == input.length()) break
                }
                end--
            }
            require(end >= floor) { "Malformed ZIP directory" }
            input.seek(end + 4)
            require(u16() == 0 && u16() == 0) { "Split ZIPs are unsupported" }
            val diskEntries = u16(); val count = u16()
            require(count == diskEntries && count != 65535 && count <= maxEntries) { "ZIP entry limit exceeded" }
            val size = u32(); val offset = u32()
            require(offset != 0xffffffffL && size != 0xffffffffL && offset + size <= end) { "Invalid/ZIP64 directory" }
            input.seek(offset)
            repeat(count) {
                val start = input.filePointer
                require(u32() == 0x02014b50L) { "Invalid ZIP entry" }
                input.seek(start + 8); require(u16() and 1 == 0) { "Encrypted ZIPs are unsupported" }
                input.seek(start + 28)
                val nameLength = u16(); val extraLength = u16(); val commentLength = u16()
                input.seek(start + 38)
                val type = ((u32() shr 16).toInt() and 0xf000)
                require(type == 0 || type == 0x8000 || type == 0x4000) { "ZIP links/special files are forbidden" }
                input.seek(start + 46 + nameLength + extraLength + commentLength)
                require(input.filePointer <= offset + size) { "Truncated ZIP directory" }
            }
            require(input.filePointer == offset + size) { "Malformed ZIP directory size" }
        }
    }

    suspend fun extract(archive: File, destination: File, nested: Boolean = false,
        limits: ExtractionLimits = ExtractionLimits(), include: (String) -> Boolean = { true },
        progress: (String) -> Unit = {}) {
        var bytes = 0L; var count = 0
        suspend fun unpack(file: File, root: File, depth: Int) {
            currentCoroutineContext().ensureActive()
            require(depth <= limits.depth) { "ZIP nesting limit exceeded" }
            inspect(file, limits.entries - count)
            require(!root.exists() && root.mkdirs()) { "Extraction destination must be new" }
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                val seen = hashSetOf<String>()
                while (entries.hasMoreElements()) {
                    currentCoroutineContext().ensureActive()
                    val entry = entries.nextElement()
                    require(++count <= limits.entries) { "ZIP entry limit exceeded" }
                    val target = contained(root, entry.name)
                    require(seen.add(target.path)) { "Duplicate ZIP entry" }
                    require(entry.size >= 0 && entry.size <= limits.bytes - bytes) { "ZIP size limit exceeded" }
                    if (entry.isDirectory) { require(target.mkdirs() || target.isDirectory); continue }
                    // Count and verify even filtered entries, preventing hidden decompression bombs.
                    val crc = java.util.zip.CRC32()
                    var entryBytes = 0L
                    val keep = include(entry.name)
                    if (keep) require(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
                    val output = if (keep) target.outputStream() else null
                    try {
                        zip.getInputStream(entry).use { input ->
                            val buffer = ByteArray(32768)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer); if (read < 0) break
                                bytes += read; entryBytes += read
                                require(bytes <= limits.bytes) { "ZIP size limit exceeded" }
                                crc.update(buffer, 0, read); output?.write(buffer, 0, read)
                            }
                        }
                    } finally { output?.close() }
                    require(entryBytes == entry.size && crc.value == entry.crc) { "Truncated or corrupt ZIP entry" }
                    progress(entry.name)
                    if (keep && nested && target.extension.equals("zip", true)) {
                        unpack(target, File(target.parentFile, target.nameWithoutExtension), depth + 1)
                        require(target.delete())
                    }
                }
            }
        }
        require(!destination.exists()) { "Extraction destination must be new" }
        try { unpack(archive, destination, 0) }
        catch (error: Throwable) { destination.deleteRecursively(); throw error }
    }
}
