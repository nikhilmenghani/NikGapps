package com.nikgapps.download

import android.os.Environment
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

class FileDownloadStrategy : DownloadStrategy {
    private val client = OkHttpClient()

    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        return downloadFile(downloadUrl, destFilePath)
    }

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
}
