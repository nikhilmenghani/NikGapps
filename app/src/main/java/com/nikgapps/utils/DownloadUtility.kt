package com.nikgapps.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import android.os.Environment
import java.io.File
import okio.buffer
import okio.sink

object DownloadUtility {
    private val client = OkHttpClient()

    fun downloadFile(url: String, destFilePath: String? = null): Boolean {
        return try {
            // Extract the zip name from the URL by splitting the URL by '/' and finding the part ending with .zip
            val zipFileName = url.split("/").lastOrNull { it.endsWith(".zip") }
                ?: throw IllegalArgumentException("No .zip file found in URL")

            // Determine the destination file path
            val destinationPath = destFilePath ?: "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileName"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.let { responseBody ->
                    val file = File(destinationPath)
                    file.sink().buffer().use { bufferedSink ->
                        bufferedSink.writeAll(responseBody.source())
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
