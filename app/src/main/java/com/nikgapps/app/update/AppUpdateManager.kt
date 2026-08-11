package com.nikgapps.app.update

import android.content.Context
import android.os.Environment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.app.utils.worker.DownloadWorker
import java.io.File
import java.util.concurrent.TimeUnit

object AppUpdateManager {
    private const val PERIODIC_WORK = "nikgapps_periodic_update_check"
    private const val STARTUP_WORK = "nikgapps_startup_update_check"
    private const val DOWNLOAD_WORK = "nikgapps_app_update_download"

    fun scheduleChecks(context: Context, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        if (intervalHours <= 0) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).setConstraints(networkConstraints()).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun checkOnAppStart(context: Context) {
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(networkConstraints()).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_WORK, ExistingWorkPolicy.REPLACE, request
        )
    }

    fun enqueueDownload(context: Context, version: String, url: String): java.util.UUID {
        val directory = checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
        val destination = File(directory, "NikGapps-v$version.apk")
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(
            workDataOf(
                DownloadWorker.DOWNLOAD_URL_KEY to url,
                DownloadWorker.DEST_FILE_PATH_KEY to destination.absolutePath,
                DownloadWorker.DOWNLOAD_TYPE_KEY to DownloadWorker.DOWNLOAD_TYPE_APK,
                DownloadWorker.VERSION_KEY to version
            )
        ).setConstraints(networkConstraints()).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DOWNLOAD_WORK, ExistingWorkPolicy.REPLACE, request
        )
        return request.id
    }

    fun downloadedApk(context: Context, version: String): File {
        val directory = checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
        return File(directory, "NikGapps-v$version.apk")
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED).build()
}
