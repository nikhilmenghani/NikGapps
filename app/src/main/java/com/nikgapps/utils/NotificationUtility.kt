package com.nikgapps.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nikgapps.R

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
fun showProgressNotification(context: Context, progress: Int, progressText: String = "Download in progress") {
    val notificationManager = NotificationManagerCompat.from(context)

    val builder = NotificationCompat.Builder(context, "progress_channel_id")
        .setContentTitle("File Download")
        .setContentText(progressText)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOnlyAlertOnce(true) // Ensures no repetitive sound for each progress update
        .setProgress(100, progress, false)

    notificationManager.notify(1, builder.build())

    // Optional: Show a completed notification when finished
    if (progress == 100) {
        builder.setContentText("Download complete")
            .setProgress(0, 0, false)
        notificationManager.notify(1, builder.build())
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Progress Channel"
        val descriptionText = "Notification channel for progress updates"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("progress_channel_id", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}


