package com.nikgapps.app.utils.download

import com.nikgapps.app.registry.SafeFilePublication
import com.nikgapps.app.registry.ArtifactDownloader
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FilterInputStream
import java.util.concurrent.TimeUnit

class VerifiedDownload(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).callTimeout(5, TimeUnit.MINUTES).build()) {
    suspend fun download(url: String, destination: File, expectedSize: Long? = null, sha256: String? = null,
        validate: (File) -> Unit = {},
        progress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val caller = currentCoroutineContext()
        val call = client.newCall(Request.Builder().url(url).build())
        coroutineScope {
            val watcher = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
                call.execute().use { response ->
                    check(response.isSuccessful) { "Download HTTP ${response.code}" }
                    val size = expectedSize ?: response.body.contentLength().takeIf { it >= 0 }
                    var read = 0L
                    val input = object : FilterInputStream(response.body.byteStream()) {
                        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                            caller.ensureActive()
                            val count = super.read(bytes, offset, length)
                            if (count > 0) { read += count; size?.takeIf { it > 0 }?.let { progress((read.toFloat() / it).coerceIn(0f, 1f)) } }
                            return count
                        }
                    }
                    SafeFilePublication.write(destination, { input }, { staged ->
                        require(staged.length() > 0 && (size == null || staged.length() == size)) { "Incomplete download" }
                        sha256?.let { require(ArtifactDownloader.sha256(staged).equals(it, true)) { "Download checksum mismatch" } }
                        SafeFilePublication.validZip(staged) { caller.ensureActive() }
                        validate(staged)
                    }, checkpoint = { caller.ensureActive() })
                }
                true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { caller.ensureActive(); false }
            finally { watcher.cancel() }
        }
    }
}
