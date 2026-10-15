package com.nikgapps.app.registry

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import com.nikgapps.app.utils.AppDiagnostics
import com.nikgapps.app.analytics.AppAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ZipPublisher(private val context: Context, private val checkpoint: (String) -> Unit = {}) {
    enum class ConflictResolution { REPLACE, RENAME }

    fun exists(fileName: String): Boolean = existingUri(fileName) != null

    suspend fun publish(
        source: File,
        packageCount: Int,
        conflictResolution: ConflictResolution = ConflictResolution.RENAME,
        operationId: String? = null
    ): String = withContext(Dispatchers.IO) {
      val caller = currentCoroutineContext()
      synchronized(PUBLICATION_LOCK) {
        caller.ensureActive()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) recoverPublication()
        operationId?.let { id ->
            context.getSharedPreferences("build-operations", Context.MODE_PRIVATE).getString("publication:$id", null)?.let { saved ->
                check(exists(saved.substringAfterLast('/'))) { "Previously published output is missing; start a new build explicitly" }
                com.nikgapps.app.data.BuildQuotaRepository(context).recordSuccess(operationId = id)
                return@synchronized saved
            }
        }
        require(source.name == File(source.name).name && source.isFile)
        SafeFilePublication.validZip(source) { caller.ensureActive() }
        val fileName = when {
            !exists(source.name) -> source.name
            conflictResolution == ConflictResolution.REPLACE -> source.name
            else -> uniqueName(source.name)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NikGapps").apply { mkdirs() }
            val output = File(directory, fileName)
            SafeFilePublication.write(output, { source.inputStream() }, {
                check(it.length() == source.length() && ArtifactDownloader.sha256(it) == ArtifactDownloader.sha256(source))
                SafeFilePublication.validZip(it)
            }, checkpoint = { caller.ensureActive() })
            AppDiagnostics.info("publication", "completed", mapOf("file" to source.name,
                "bytes" to source.length(), "destination" to "Downloads/NikGapps"))
            AppAnalytics.zipCreationSucceeded(
                fileName, packageCount, source.length(), conflictResolution.name.lowercase()
            )
            return@synchronized completePublication(output.absolutePath, operationId)
        }
        val previous = if (conflictResolution == ConflictResolution.REPLACE) existingUri(fileName) else null
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, ".nikgapps-${java.util.UUID.randomUUID()}.zip"); put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NikGapps"); put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create Downloads entry")
        var oldRenamed = false
        var promoted = false
        val journal = context.getSharedPreferences("zip-publication", Context.MODE_PRIVATE)
        try {
            checkpoint("before-write")
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { input ->
                val buffer = ByteArray(32768)
                while (true) { caller.ensureActive(); val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count) }
            } }
                ?: error("Cannot write Downloads entry")
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            var size = 0L
            (resolver.openInputStream(uri) ?: error("Cannot verify staged Downloads entry")).use { input ->
                val buffer = ByteArray(32768)
                while (true) { caller.ensureActive()
                    val count = input.read(buffer); if (count < 0) break; size += count; digest.update(buffer, 0, count) }
            }
            check(size == source.length() && digest.digest().joinToString("") { "%02x".format(it) } == ArtifactDownloader.sha256(source)) { "Staged ZIP verification failed" }
            caller.ensureActive()
            // Record the old URI before changing provider metadata. A crash leaves its bytes intact.
            check(journal.edit().putString("old", previous?.toString()).putString("name", fileName)
                .putString("stage", uri.toString()).putBoolean("promoted", false).commit())
            if (previous != null) {
                val backup = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, ".nikgapps-backup-${java.util.UUID.randomUUID()}-$fileName") }
                check(resolver.update(previous, backup, null, null) == 1) { "Cannot back up previous Downloads entry" }
                oldRenamed = true
                checkpoint("after-backup")
            }
            values.clear(); values.put(MediaStore.Downloads.DISPLAY_NAME, fileName); values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1) { "Cannot publish Downloads entry" }
            promoted = true
            checkpoint("after-promotion")
            check(journal.edit().putBoolean("promoted", true).commit())
            val location = completePublication("Downloads/NikGapps/$fileName", operationId)
            previous?.let { runCatching { resolver.delete(it, null, null) } }
            journal.edit().clear().commit()
            AppDiagnostics.info("publication", "completed", mapOf("file" to source.name,
                "bytes" to source.length(), "destination" to "Downloads/NikGapps"))
            AppAnalytics.zipCreationSucceeded(
                fileName, packageCount, source.length(), conflictResolution.name.lowercase()
            )
            return@synchronized location
        } catch (e: Exception) {
            AppDiagnostics.failure("publication", "failed", e, mapOf("file" to source.name))
            AppAnalytics.zipCreationFailed(fileName, packageCount, source.length(), e)
            if (!promoted) {
                if (oldRenamed && previous != null) {
                    val restore = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, fileName) }
                    runCatching { check(resolver.update(previous, restore, null, null) == 1) }
                }
                runCatching { resolver.delete(uri, null, null) }
            }
            throw e
        }
    }
    }

    companion object { private val PUBLICATION_LOCK = Any() }

    private fun completePublication(location: String, operationId: String?): String {
        if (operationId != null) {
            check(context.getSharedPreferences("build-operations", Context.MODE_PRIVATE).edit()
                .putString("publication:$operationId", location).commit()) { "Cannot persist publication receipt" }
            com.nikgapps.app.data.BuildQuotaRepository(context).recordSuccess(operationId = operationId)
        }
        return location
    }

    @androidx.annotation.RequiresApi(29)
    private fun recoverPublication() {
        val journal = context.getSharedPreferences("zip-publication", Context.MODE_PRIVATE)
        val stage = journal.getString("stage", null)?.let(android.net.Uri::parse) ?: return
        val old = journal.getString("old", null)?.let(android.net.Uri::parse)
        val resolver = context.contentResolver
        if (journal.getBoolean("promoted", false)) {
            old?.let { resolver.delete(it, null, null) }
        } else {
            if (old != null) {
                val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, journal.getString("name", null)) }
                check(resolver.update(old, values, null, null) == 1) { "Previous ZIP remains preserved at $old; recovery required" }
            }
            resolver.delete(stage, null, null)
        }
        check(journal.edit().clear().commit())
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
