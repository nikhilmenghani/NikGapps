package com.nikgapps.app.utils.download

//noinspection SuspiciousImport
import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import com.nikgapps.app.utils.NotificationUtility.createNotificationChannel
import com.nikgapps.app.utils.NotificationUtility.showProgressNotification
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FileDownloadStrategy() : DownloadStrategy {

    private val client = OkHttpClient()

    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        return downloadFileWithProgress(downloadUrl, destFilePath)
    }

    private suspend fun downloadFileWithProgress(url: String, destFilePath: String?): Boolean {
        return suspendCoroutine { continuation ->
            try {
                // Extract the zip name from the URL by splitting the URL by '/' and finding the part ending with .zip
                val zipFileName = url.split("/").lastOrNull { it.endsWith(".zip") }
                    ?: throw IllegalArgumentException("No .zip file found in URL")

                // Determine the destination file path
                val destinationPath = destFilePath
                    ?: "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileName"

                val request = Request.Builder().url(url).build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                        continuation.resume(false)
                    }

                    @SuppressLint("MissingPermission")
                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            continuation.resume(false)
                            return
                        }

                        val responseBody = response.body ?: run {
                            continuation.resume(false)
                            return
                        }

                        val file = File(destinationPath)
                        try {
                            val contentLength = responseBody.contentLength()
                            var totalBytesRead: Long = 0
                            createNotificationChannel()
                            file.sink().buffer().use { bufferedSink ->
                                val source = responseBody.source()
                                var bytesRead: Long
                                val buffer = Buffer()
                                while (source.read(buffer, 8192).also { bytesRead = it } != -1L) {
                                    bufferedSink.write(buffer, bytesRead)
                                    totalBytesRead += bytesRead
                                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                                    showProgressNotification(
                                        progress = progress,
                                        contentTitle = "Downloading NikGapps Build",
                                        progressText = "Downloading $zipFileName",
                                        completeText = "$zipFileName downloaded successfully"
                                    )
                                }
                            }
                            Log.d("FileDownloadStrategy", "Download complete")
                            continuation.resume(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continuation.resume(false)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("FileDownloadStrategy", "Error downloading file", e)
                e.printStackTrace()
                continuation.resume(false)
            }
        }
    }
}
