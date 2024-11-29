package com.nikgapps.app.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nikgapps.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nikgapps.R

object NotificationUtility {

    const val CHANNEL_ID = "progress_channel_id"
    const val NOTIFICATION_ID = 1

    fun startFileDownload(context: Context) {
        createNotificationChannel(context)

        val totalProgress = 100

        // Simulate a download with a coroutine (replace with actual logic)
        CoroutineScope(Dispatchers.IO).launch {
            for (progress in 0..totalProgress step 10) {
                delay(500) // Simulate download delay
                showProgressNotification(context, progress)
            }
        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    fun showProgressNotification(
        context: Context = App.globalClass,
        progress: Int,
        progressText: String = "Download in progress",
        channelId: String = CHANNEL_ID,
        contentTitle: String = "File Download",
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        completeText: String = "Download complete",
        notificationId: Int = NOTIFICATION_ID
    ) {
        val notificationManager = NotificationManagerCompat.from(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(contentTitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(priority)
            .setOnlyAlertOnce(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Ensures sound, vibration, etc.
            .setCategory(NotificationCompat.CATEGORY_PROGRESS) // Explicitly set category

        if (progress < 100) {
            builder.setContentText("$progressText: $progress%")
                .setProgress(100, progress, false)
        } else {
            notificationManager.cancel(notificationId)
            builder.setContentText(completeText)
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
        }
        notificationManager.notify(notificationId, builder.build())
    }


    fun createNotificationChannel(
        context: Context = App.globalClass,
        name: String = "Progress Channel",
        descriptionText: String = "Notification channel for progress updates",
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        channelId: String = CHANNEL_ID
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

