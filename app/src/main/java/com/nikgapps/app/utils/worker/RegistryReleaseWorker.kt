package com.nikgapps.app.utils.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.nikgapps.MainActivity
import com.nikgapps.R
import com.nikgapps.app.data.ReleaseNoticeRepository
import com.nikgapps.app.registry.CatalogRepository
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class RegistryReleaseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val metadata = CatalogRepository(applicationContext.cacheDir).load(forceRefresh = true)
        if (metadata.fromCache) return Result.retry()
        val index = metadata.releaseIndex ?: return Result.success()
        val latest = index.latest.values.flatMap { it.values.flatMap { architecture -> architecture.values } }.toSet()
        val releases = index.releases.filter { it.id in latest && it.channel == "stable" }
        val notices = ReleaseNoticeRepository(applicationContext)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "NikGapps releases", NotificationManager.IMPORTANCE_DEFAULT))
        val permitted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!permitted || !manager.areNotificationsEnabled()) return Result.success()
        val firstCheck = !notices.notificationBaselineEstablished()
        val candidates = if (firstCheck) listOfNotNull(releases.maxByOrNull { it.createdAt }) else releases
        candidates.filter { !notices.notified(it.id) && !notices.dismissed(it.id) }.forEach { release ->
            val open = PendingIntent.getActivity(applicationContext, release.id.hashCode(),
                Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(release.id.hashCode(), NotificationCompat.Builder(applicationContext, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("A new NikGapps release is available")
                .setContentText("Updated packages and fixes for Android ${release.androidVersion}.")
                .setContentIntent(open).setAutoCancel(true).build())
            notices.markNotified(release.id)
        }
        if (firstCheck) {
            // Do not announce years of old releases when this feature is first enabled.
            releases.forEach { notices.markNotified(it.id) }
            notices.establishNotificationBaseline()
        }
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { Result.retry() }

    companion object {
        private const val CHANNEL = "nikgapps_registry_releases"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RegistryReleaseWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("registry-releases", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
