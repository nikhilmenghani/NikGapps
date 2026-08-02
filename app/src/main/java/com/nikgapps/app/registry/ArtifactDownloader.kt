package com.nikgapps.app.registry

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class DownloadProgress(val packageId: String, val downloaded: Long, val total: Long?)
class ArtifactDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ArtifactDownloader(private val cacheDirectory: File, private val client: OkHttpClient = OkHttpClient(),
    private val maxAttempts: Int = 3) {
    suspend fun obtain(pkg: ResolvedPackage, onProgress: suspend (DownloadProgress) -> Unit = {}): File =
        withContext(Dispatchers.IO) {
            val artifact = pkg.version.artifact
            val directory = File(cacheDirectory, "nikgapps/packages").apply { mkdirs() }
            val target = File(directory, "${artifact.sha256}.zip")
            if (target.isFile) {
                if (sha256(target) == artifact.sha256 && (artifact.size == null || target.length() == artifact.size)) return@withContext target
                target.delete()
            }
            val part = File(directory, "${artifact.sha256}.part")
            var last: Throwable? = null
            repeat(maxAttempts) { attempt ->
                try {
                    download(pkg.catalogPackage.id, artifact, part, onProgress)
                    if (artifact.size != null && part.length() != artifact.size) throw ArtifactDownloadException(
                        "Size mismatch for '${pkg.catalogPackage.id}': expected ${artifact.size}, got ${part.length()}")
                    val actual = sha256(part)
                    if (actual != artifact.sha256) throw ArtifactDownloadException(
                        "Checksum mismatch for '${pkg.catalogPackage.id}': expected ${artifact.sha256}, got $actual")
                    if (!part.renameTo(target)) throw ArtifactDownloadException("Cannot commit '${pkg.catalogPackage.id}' to artifact cache")
                    return@withContext target
                } catch (e: Throwable) {
                    last = e
                    if (e is ArtifactDownloadException && (e.message?.contains("mismatch") == true)) part.delete()
                    if (attempt + 1 < maxAttempts) delay(500L * (attempt + 1))
                }
            }
            part.delete()
            throw ArtifactDownloadException("Download failed for '${pkg.catalogPackage.id}' after $maxAttempts attempts: ${last?.message}", last)
        }

    private suspend fun download(id: String, artifact: Artifact, part: File, progress: suspend (DownloadProgress) -> Unit) {
        val offset = part.takeIf { it.isFile }?.length() ?: 0L
        val request = Request.Builder().url(artifact.url).apply { if (offset > 0) header("Range", "bytes=$offset-") }.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ArtifactDownloadException("HTTP ${response.code} while downloading '$id'")
            val append = offset > 0 && response.code == 206
            if (offset > 0 && !append) part.delete()
            val start = if (append) offset else 0L
            val body = response.body
            RandomAccessFile(part, "rw").use { output ->
                output.seek(start); if (!append) output.setLength(0)
                body.byteStream().use { input ->
                    val buffer = ByteArray(128 * 1024); var downloaded = start
                    while (true) { val count = input.read(buffer); if (count < 0) break
                        output.write(buffer, 0, count); downloaded += count
                        progress(DownloadProgress(id, downloaded, artifact.size))
                    }
                }
            }
        }
    }
    companion object {
        fun sha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input -> val buffer = ByteArray(128 * 1024)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
