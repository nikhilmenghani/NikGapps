package com.nikgapps.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

object DownloadUtility {
    private val client = OkHttpClient()

    /**
     * Downloads a file from the given URL to the specified destination file path.
     *
     * @param url The URL from which to download the file.
     * @param destFilePath The file path where the downloaded file should be saved.
     * @return True if the file was downloaded successfully, False otherwise.
     */
    fun downloadFile(url: String, destFilePath: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.let { responseBody ->
                    val file = File(destFilePath)
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
