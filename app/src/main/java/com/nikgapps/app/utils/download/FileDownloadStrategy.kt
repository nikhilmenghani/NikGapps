package com.nikgapps.app.utils.download

import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nikgapps.App
import okhttp3.*
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FileDownloadStrategy() : DownloadStrategy {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1
    }

    private val client = OkHttpClient()

    override suspend fun download(downloadUrl: String, destFilePath: String): Boolean {
        createNotificationChannel()
        return downloadFileWithProgress(downloadUrl, destFilePath)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Channel"
            val descriptionText = "Channel for file download progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                App.globalClass.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun downloadFileWithProgress(url: String, destFilePath: String?): Boolean {
        return suspendCoroutine { continuation ->
            try {
                // Extract the zip name from the URL by splitting the URL by '/' and finding the part ending with .zip
                val zipFileName = url.split("/").lastOrNull { it.endsWith(".zip") }
                    ?: throw IllegalArgumentException("No .zip file found in URL")

                // Determine the destination file path
                val destinationPath = destFilePath ?: "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileName"

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

                            val notificationBuilder = NotificationCompat.Builder(App.globalClass, CHANNEL_ID)
                                .setSmallIcon(R.drawable.stat_sys_download)
                                .setContentTitle("Downloading file")
                                .setContentText("Download in progress")
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                                .setProgress(100, 0, false)

                            NotificationManagerCompat.from(App.globalClass).notify(NOTIFICATION_ID, notificationBuilder.build())

                            file.sink().buffer().use { bufferedSink ->
                                val source = responseBody.source()
                                var bytesRead: Long
                                val buffer = Buffer()
                                while (source.read(buffer, 8192).also { bytesRead = it } != -1L) {
                                    bufferedSink.write(buffer, bytesRead)
                                    totalBytesRead += bytesRead

                                    // Update progress
                                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                                    notificationBuilder.setProgress(100, progress, false)
                                    NotificationManagerCompat.from(App.globalClass).notify(NOTIFICATION_ID, notificationBuilder.build())
                                }
                            }

                            // Mark the download as complete
                            notificationBuilder.setContentText("Download complete")
                                .setProgress(0, 0, false)
                                .setSmallIcon(R.drawable.stat_sys_download_done)
                            NotificationManagerCompat.from(App.globalClass).notify(NOTIFICATION_ID, notificationBuilder.build())

                            continuation.resume(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continuation.resume(false)
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(false)
            }
        }
    }
}
