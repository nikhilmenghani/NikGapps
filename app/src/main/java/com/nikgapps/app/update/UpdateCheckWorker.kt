package com.nikgapps.app.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikgapps.app.utils.NotificationUtility
import com.nikgapps.app.utils.constants.ApplicationConstants
import com.nikgapps.app.utils.network.VersionFetcher
import com.nikgapps.dumps.getCurrentVersion

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val latest = VersionFetcher.fetchLatestVersion()
        if (latest == "Unknown") return Result.retry()
        if (!isNewer(latest, getCurrentVersion(applicationContext))) return Result.success()
        NotificationUtility.showUpdateAvailable(
            applicationContext,
            latest,
            ApplicationConstants.getNikGappsAppDownloadUrl(latest)
        )
        return Result.success()
    }

    private fun isNewer(candidate: String, installed: String): Boolean {
        val left = candidate.trimStart('v').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val right = installed.trimStart('v').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(left.size, right.size)) { index ->
            val comparison = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }
}
