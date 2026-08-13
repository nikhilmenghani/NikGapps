package com.nikgapps.app.utils.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nikgapps.app.utils.download.ApkDownloadStrategy
import com.nikgapps.app.utils.download.DownloadStrategy
import com.nikgapps.app.utils.download.FileDownloadStrategy
import com.nikgapps.app.utils.NotificationUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val DOWNLOAD_URL_KEY = "DOWNLOAD_URL"
        const val DEST_FILE_PATH_KEY = "DEST_FILE_PATH"
        const val DOWNLOAD_TYPE_KEY = "DOWNLOAD_TYPE"
        const val DOWNLOAD_TYPE_APK = "apk"
        const val DOWNLOAD_TYPE_FILE = "file"
        const val VERSION_KEY = "VERSION"
        const val PROGRESS_KEY = "DOWNLOAD_PROGRESS"
        const val OUTPUT_APK_PATH_KEY = "OUTPUT_APK_PATH"
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(DOWNLOAD_URL_KEY)
        val destFilePath = inputData.getString(DEST_FILE_PATH_KEY)
        val downloadType = inputData.getString(DOWNLOAD_TYPE_KEY)

        if (downloadUrl.isNullOrEmpty() || destFilePath.isNullOrEmpty()) {
            Log.e("NikGapps-DownloadWorker", "Invalid input data: URL or destination path is missing.")
            return Result.failure()
        }

        // Instantiate the appropriate download strategy
        val downloadStrategy: DownloadStrategy = when (downloadType) {
            DOWNLOAD_TYPE_APK -> ApkDownloadStrategy()
            DOWNLOAD_TYPE_FILE -> FileDownloadStrategy()
            else -> {
                Log.e("NikGapps-DownloadWorker", "Invalid download type specified.")
                return Result.failure()
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val version = inputData.getString(VERSION_KEY).orEmpty().ifBlank { "update" }
                if (downloadType == DOWNLOAD_TYPE_APK) setForeground(downloadForegroundInfo())
                val downloadSuccess = if (downloadStrategy is ApkDownloadStrategy) {
                    downloadStrategy.downloadApk(downloadUrl, destFilePath) { fraction ->
                        val progress = (fraction * 100).toInt().coerceIn(0, 100)
                        setProgressAsync(workDataOf(PROGRESS_KEY to progress))
                        NotificationUtility.showProgressNotification(
                            context = applicationContext,
                            progress = progress,
                            progressText = "Downloading app update",
                            contentTitle = "Downloading NikGapps v$version",
                            priority = androidx.core.app.NotificationCompat.PRIORITY_LOW,
                            notificationId = NotificationUtility.UPDATE_DOWNLOAD_NOTIFICATION_ID
                        )
                    }
                } else downloadStrategy.download(downloadUrl, destFilePath)
                if (downloadSuccess) {
                    Log.d("NikGapps-DownloadWorker", "Download successful: $destFilePath")
                    if (downloadType == DOWNLOAD_TYPE_APK) {
                        NotificationUtility.showUpdateReady(
                            applicationContext,
                            version,
                            destFilePath
                        )
                    }
                    Result.success(workDataOf(OUTPUT_APK_PATH_KEY to destFilePath))
                } else {
                    Log.e("NikGapps-DownloadWorker", "Download failed.")
                    if (runAttemptCount < 2) Result.retry() else Result.failure()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("NikGapps-DownloadWorker", "Exception during download: ${e.message}")
                if (runAttemptCount < 2) Result.retry() else Result.failure()
            }
        }
    }

    private fun downloadForegroundInfo(): ForegroundInfo {
        NotificationUtility.createNotificationChannel(
            context = applicationContext,
            name = "App update downloads",
            descriptionText = "NikGapps update download progress",
            importance = android.app.NotificationManager.IMPORTANCE_LOW
        )
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NotificationUtility.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading NikGapps update")
            .setContentText("Starting download")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()
        return ForegroundInfo(
            NotificationUtility.UPDATE_DOWNLOAD_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
