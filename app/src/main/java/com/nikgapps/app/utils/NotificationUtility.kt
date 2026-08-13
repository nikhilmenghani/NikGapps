package com.nikgapps.app.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nikgapps.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nikgapps.R
import com.nikgapps.MainActivity
import com.nikgapps.app.update.UpdateActionReceiver

object NotificationUtility {

    const val CHANNEL_ID = "progress_channel_id"
    const val NOTIFICATION_ID = 1
    const val UPDATE_AVAILABLE_NOTIFICATION_ID = 2202
    const val UPDATE_READY_NOTIFICATION_ID = 2203
    const val UPDATE_DOWNLOAD_NOTIFICATION_ID = 2204
    private const val UPDATE_CHANNEL_ID = "app_updates"

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

    @SuppressLint("MissingPermission")
    fun showUpdateAvailable(context: Context, version: String, downloadUrl: String) {
        createNotificationChannel(context, "App updates", "New versions and installation status",
            NotificationManager.IMPORTANCE_DEFAULT, UPDATE_CHANNEL_ID)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = UpdateActionReceiver.ACTION_DOWNLOAD
            putExtra(UpdateActionReceiver.EXTRA_VERSION, version)
            putExtra(UpdateActionReceiver.EXTRA_URL, downloadUrl)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val action = PendingIntent.getActivity(context, version.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("NikGapps v$version is available")
            .setContentText("Tap once to download and open the installer")
            .setContentIntent(action).addAction(android.R.drawable.stat_sys_download, "Download & install", action)
            .setAutoCancel(true).setOnlyAlertOnce(true).build()
        runCatching { NotificationManagerCompat.from(context).notify(UPDATE_AVAILABLE_NOTIFICATION_ID, notification) }
    }

    @SuppressLint("MissingPermission")
    fun showUpdateReady(context: Context, version: String, apkPath: String) {
        createNotificationChannel(context, "App updates", "New versions and installation status",
            NotificationManager.IMPORTANCE_DEFAULT, UPDATE_CHANNEL_ID)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = UpdateActionReceiver.ACTION_INSTALL
            putExtra(UpdateActionReceiver.EXTRA_APK_PATH, apkPath)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val action = PendingIntent.getActivity(context, apkPath.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("NikGapps v$version is ready")
            .setContentText("Tap to review and install")
            .setContentIntent(action).addAction(android.R.drawable.stat_sys_download_done, "Install", action)
            .setAutoCancel(true).build()
        runCatching {
            NotificationManagerCompat.from(context).apply {
                cancel(UPDATE_DOWNLOAD_NOTIFICATION_ID)
                notify(UPDATE_READY_NOTIFICATION_ID, notification)
            }
        }
    }
}

