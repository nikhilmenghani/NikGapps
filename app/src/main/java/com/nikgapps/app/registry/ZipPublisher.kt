package com.nikgapps.app.registry

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import com.nikgapps.app.utils.AppDiagnostics

class ZipPublisher(private val context: Context) {
    fun publish(source: File): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NikGapps").apply { mkdirs() }
            val output = File(directory, source.name)
            source.inputStream().use { input -> output.outputStream().use(input::copyTo) }
            AppDiagnostics.info("publication", "completed", mapOf("file" to source.name,
                "bytes" to source.length(), "destination" to "Downloads/NikGapps"))
            return output.absolutePath
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.name); put(MediaStore.Downloads.MIME_TYPE, "application/zip")
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
            return "Downloads/NikGapps/${source.name}"
        } catch (e: Exception) {
            AppDiagnostics.failure("publication", "failed", e, mapOf("file" to source.name))
            resolver.delete(uri, null, null); throw e
        }
    }
}
