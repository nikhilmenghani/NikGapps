package com.nikgapps.download

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.File

class ApkDownloadStrategy : DownloadStrategy {
    private val client = OkHttpClient()

    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        return downloadApk(downloadUrl, destFilePath)
    }

    fun downloadApk(url: String, destFilePath: String, onProgressUpdate: ((Float) -> Unit)? = null): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.let { responseBody ->
                    val totalBytes = responseBody.contentLength()
                    var downloadedBytes: Long = 0

                    val file = File(destFilePath)
                    file.sink().buffer().use { bufferedSink ->
                        responseBody.source().use { source ->
                            val buffer = okio.Buffer()
                            var bytesRead: Long

                            while (source.read(buffer, 8 * 1024).also { bytesRead = it } != -1L) {
                                bufferedSink.write(buffer, bytesRead)
                                downloadedBytes += bytesRead

                                // Notify progress if required
                                onProgressUpdate?.invoke(downloadedBytes.toFloat() / totalBytes)
                            }
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}