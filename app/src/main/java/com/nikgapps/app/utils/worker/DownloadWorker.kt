package com.nikgapps.app.utils.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
                val downloadSuccess = downloadStrategy.download(downloadUrl, destFilePath)
                if (downloadSuccess) {
                    Log.d("NikGapps-DownloadWorker", "Download successful: $destFilePath")
                    if (downloadType == DOWNLOAD_TYPE_APK) {
                        NotificationUtility.showUpdateReady(
                            applicationContext,
                            inputData.getString(VERSION_KEY).orEmpty(),
                            destFilePath
                        )
                    }
                    Result.success()
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
}
