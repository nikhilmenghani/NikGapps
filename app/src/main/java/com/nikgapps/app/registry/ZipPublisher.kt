package com.nikgapps.app.registry

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import com.nikgapps.app.utils.AppDiagnostics

class ZipPublisher(private val context: Context) {
    enum class ConflictResolution { REPLACE, RENAME }

    fun exists(fileName: String): Boolean = existingUri(fileName) != null

    fun publish(source: File, conflictResolution: ConflictResolution = ConflictResolution.RENAME): String {
        val fileName = when {
            !exists(source.name) -> source.name
            conflictResolution == ConflictResolution.REPLACE -> source.name
            else -> uniqueName(source.name)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NikGapps").apply { mkdirs() }
            val output = File(directory, fileName)
            source.inputStream().use { input -> output.outputStream().use(input::copyTo) }
            AppDiagnostics.info("publication", "completed", mapOf("file" to source.name,
                "bytes" to source.length(), "destination" to "Downloads/NikGapps"))
            return output.absolutePath
        }
        if (conflictResolution == ConflictResolution.REPLACE) {
            existingUri(fileName)?.let { context.contentResolver.delete(it, null, null) }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName); put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NikGapps"); put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create Downloads entry")
        try {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Cannot write Downloads entry")
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); resolver.update(uri, values, null, null)
            AppDiagnostics.info("publication", "completed", mapOf("file" to source.name,
                "bytes" to source.length(), "destination" to "Downloads/NikGapps"))
            return "Downloads/NikGapps/$fileName"
        } catch (e: Exception) {
            AppDiagnostics.failure("publication", "failed", e, mapOf("file" to source.name))
            resolver.delete(uri, null, null); throw e
        }
    }

    private fun uniqueName(original: String): String {
        val stem = original.removeSuffix(".zip")
        var number = 1
        while (exists("$stem ($number).zip")) number++
        return "$stem ($number).zip"
    }

    private fun existingUri(fileName: String): android.net.Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NikGapps")
            return if (File(directory, fileName).isFile) android.net.Uri.EMPTY else null
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val arguments = arrayOf(fileName, "${Environment.DIRECTORY_DOWNLOADS}/NikGapps/")
        return context.contentResolver.query(collection, arrayOf(MediaStore.Downloads._ID),
            selection, arguments, null)?.use { cursor ->
            if (cursor.moveToFirst()) android.content.ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
        }
    }
}
