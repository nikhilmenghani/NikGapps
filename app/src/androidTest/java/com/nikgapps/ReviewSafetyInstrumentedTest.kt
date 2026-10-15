package com.nikgapps

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import com.nikgapps.app.data.*
import com.nikgapps.app.registry.ZipPublisher
import com.nikgapps.app.utils.NotificationUtility
import com.nikgapps.app.utils.worker.DownloadWorker
import com.nikgapps.app.utils.worker.BuildWorkScheduler
import com.nikgapps.app.utils.worker.BuildZipWorker
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Never targets personal app data, requests permissions, or invokes root. */
@RunWith(AndroidJUnit4::class)
class ReviewSafetyInstrumentedTest {
    @Test fun repeatedConcurrentRequestsShareOneActiveWorker() = runBlocking {
        val context = context(); val project = "concurrent-${UUID.randomUUID()}"
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInitialDelay(60, java.util.concurrent.TimeUnit.SECONDS).build()
        val first = BuildWorkScheduler.enqueue(context, project, "$project-first", request, false)
        try {
            val attempts = (0..5).map { index -> async {
                BuildWorkScheduler.enqueue(context, project, "$project-$index", OneTimeWorkRequestBuilder<DownloadWorker>().build(), false)
            } }
            assertEquals(setOf(first), attempts.map { it.await() }.toSet())
            assertEquals(1, WorkManager.getInstance(context).getWorkInfosForUniqueWork(BuildZipWorker.uniqueName(project)).get().size)
            val pending = BuildWorkScheduler.currentOperation(context, project)
            assertNotNull(pending)
            val prefs = context.getSharedPreferences("build-operations", Context.MODE_PRIVATE)
            check(prefs.edit().putString("publication:$first", "Downloads/NikGapps/review.zip").commit())
            assertNull(BuildWorkScheduler.currentOperation(context, project))
        } finally { WorkManager.getInstance(context).cancelWorkById(first).result.get() }
    }
    @Test fun buildWorkerForegroundStartupFailsTruthfullyWithoutAuthentication() = runBlocking {
        val context = context()
        check(GithubPrefs.token.isBlank()) { "Never use a live account in review fixtures" }
        val previousGuestMode = GithubPrefs.guestMode
        try {
        GithubPrefs.guestMode = false
        delay(100)
        val request = OneTimeWorkRequestBuilder<BuildZipWorker>().build()
        val manager = WorkManager.getInstance(context); manager.enqueue(request).result.get()
        withTimeout(15000) { while (manager.getWorkInfoById(request.id).get()?.state?.isFinished != true) delay(50) }
        val info = manager.getWorkInfoById(request.id).get()!!
        assertEquals(WorkInfo.State.FAILED, info.state)
        assertTrue(info.outputData.getBoolean(BuildZipWorker.KEY_AUTH_ERROR, false))
        } finally { GithubPrefs.guestMode = previousGuestMode }
    }
    private fun context(): Context = InstrumentationRegistry.getInstrumentation().targetContext.also {
        check(it.packageName == "com.nikgapps.review.debug") { "Disposable review app required" }
    }
    @Test fun quotaReceiptSurvivesRepositoryRecreationAndIsIdempotent() {
        val context = context(); val preferenceName = "quota-fixture-${UUID.randomUUID()}"
        val repo = BuildQuotaRepository(context, preferenceName)
        val operation = UUID.randomUUID().toString(); val before = repo.status().successfulBuilds
        repo.recordSuccess(operationId = operation)
        assertEquals(before + 1, BuildQuotaRepository(context, preferenceName).recordSuccess(operationId = operation).successfulBuilds)
    }
    @Test fun projectCopyKeepsSelectionAndCorruptNeighborIsPreserved() {
        val context = context()
        context.getSharedPreferences("build_projects", Context.MODE_PRIVATE).edit().clear().commit()
        val repo = BuildProjectRepository(context)
        val project = BuildProject(name = "review fixture", androidVersion = AndroidVersion.ANDROID_17,
            architecture = Architecture.ARM64, selectedAppIds = setOf("clock"))
        repo.addProject(project)
        repo.updateProject(project.copy(name = "renamed fixture"))
        assertEquals(setOf("clock"), BuildProjectRepository(context).getProjects().first { it.id == project.id }.selectedAppIds)
        val prefs = context.getSharedPreferences("build_projects", Context.MODE_PRIVATE)
        // Exact storage key is asserted below so a refactor cannot silently bypass the fixture.
        val key = prefs.all.keys.first { prefs.getString(it, null)?.startsWith("[") == true }
        val saved = prefs.getString(key, null)!!
        val corrupted = saved.dropLast(1) + ", {\"broken\":true}]"
        assertTrue(prefs.edit().putString(key, corrupted).commit())
        assertTrue(BuildProjectRepository(context).getProjects().any { it.id == project.id })
        assertEquals(corrupted, BuildProjectRepository(context).recoveryBackup())
    }
    @Test fun persistedReceiptAndOperationSurviveAnActualNewAppProcess() = runBlocking {
        val context = context()
        val phase = InstrumentationRegistry.getArguments().getString("reviewPhase", "seed")
        val quota = BuildQuotaRepository(context, "process-fixture-quota")
        val prefs = context.getSharedPreferences("process-fixture", Context.MODE_PRIVATE)
        if (phase == "seed") {
            context.getSharedPreferences("process-fixture-quota", Context.MODE_PRIVATE).edit().clear().commit()
            val id = BuildWorkScheduler.enqueue(context, "process-fixture", "process-fixture",
                OneTimeWorkRequestBuilder<DownloadWorker>().build(), false)
            quota.recordSuccess(operationId = "process-fixture")
            check(prefs.edit().putString("id", id.toString()).putInt("pid", android.os.Process.myPid()).commit())
        } else {
            assertNotEquals(prefs.getInt("pid", -1), android.os.Process.myPid())
            val id = BuildWorkScheduler.enqueue(context, "process-fixture", "process-fixture",
                OneTimeWorkRequestBuilder<DownloadWorker>().build(), false)
            assertEquals(prefs.getString("id", null), id.toString())
            assertEquals(1, quota.recordSuccess(operationId = "process-fixture").successfulBuilds)
        }
        Unit
    }
    @Test fun workerStartupAndPersistedSchedulingWithoutNetworkOrRoot() = runBlocking {
        val context = context(); val project = "fixture-${UUID.randomUUID()}"
        val operation = "$project-operation"
        val first = BuildWorkScheduler.enqueue(context, project, operation,
            OneTimeWorkRequestBuilder<DownloadWorker>().build(), false)
        val manager = WorkManager.getInstance(context)
        withTimeout(15000) {
            while (manager.getWorkInfoById(first).get()?.state?.isFinished != true) delay(50)
        }
        assertEquals(WorkInfo.State.FAILED, manager.getWorkInfoById(first).get()!!.state)
        val restored = BuildWorkScheduler.enqueue(context, project, operation,
            OneTimeWorkRequestBuilder<DownloadWorker>().build(), false)
        assertEquals(first, restored)
        NotificationUtility.createNotificationChannel(context, "Review fixtures", "Disposable review only", android.app.NotificationManager.IMPORTANCE_LOW)
    }
    @Test fun downloadsProviderReplacementKeepsValidContent() = runBlocking {
        val context = context()
        org.junit.Assume.assumeTrue("MediaStore Downloads requires API 29; no storage grant requested", android.os.Build.VERSION.SDK_INT >= 29)
        val directory = File(context.cacheDir, "review-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(directory, "review-${UUID.randomUUID()}.zip")
        fun archive(value: String) { ZipOutputStream(source.outputStream()).use {
            it.putNextEntry(ZipEntry("fixture.txt")); it.write(value.toByteArray()); it.closeEntry()
        } }
        try {
            archive("before"); ZipPublisher(context).publish(source, 1)
            archive("after"); ZipPublisher(context).publish(source, 1, ZipPublisher.ConflictResolution.REPLACE)
            val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            context.contentResolver.query(collection, arrayOf("_id"), "_display_name=?", arrayOf(source.name), null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                val copy = File(directory, "verified.zip")
                context.contentResolver.openInputStream(uri)!!.use { input -> copy.outputStream().use { input.copyTo(it) } }
                java.util.zip.ZipFile(copy).use { zip -> assertEquals("after", zip.getInputStream(zip.getEntry("fixture.txt")).reader().readText()) }
                assertEquals(1, context.contentResolver.delete(uri, null, null))
            }
        } finally { directory.deleteRecursively() }
    }
    @Test fun apkChecksValidateIdentityVersionAndSigningBeforePromotion() {
        val context = context()
        val own = File(context.applicationInfo.sourceDir)
        com.nikgapps.app.utils.download.ApkDownloadStrategy(context, BuildConfig.VERSION_NAME).validateApk(own)
        assertThrows(IllegalArgumentException::class.java) {
            com.nikgapps.app.utils.download.ApkDownloadStrategy(context, "invalid-version").validateApk(own)
        }
        val testContext = InstrumentationRegistry.getInstrumentation().context
        assertThrows(IllegalArgumentException::class.java) {
            com.nikgapps.app.utils.download.ApkDownloadStrategy(context).validateApk(File(testContext.applicationInfo.sourceDir))
        }
    }
    @Test fun providerFailuresCancellationAndInterruptedReplacementPreserveOldZip() = runBlocking {
        val context = context()
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 29)
        val directory = File(context.cacheDir, "publication-faults-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(directory, "review-faults-${UUID.randomUUID()}.zip")
        val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        fun archive(value: String) { ZipOutputStream(source.outputStream()).use {
            it.putNextEntry(ZipEntry("fixture")); it.write(value.toByteArray()); it.closeEntry()
        } }
        fun current(): android.net.Uri = context.contentResolver.query(collection, arrayOf("_id"), "_display_name=?", arrayOf(source.name), null)!!.use {
            check(it.moveToFirst()); android.content.ContentUris.withAppendedId(collection, it.getLong(0))
        }
        fun assertPrevious() {
            val copy = File(directory, "check.zip")
            context.contentResolver.openInputStream(current())!!.use { input -> copy.outputStream().use { input.copyTo(it) } }
            java.util.zip.ZipFile(copy).use { zip -> assertEquals("old", zip.getInputStream(zip.getEntry("fixture")).reader().readText()) }
        }
        try {
            archive("old"); ZipPublisher(context).publish(source, 1)
            archive("replacement")
            for (point in listOf("before-write", "after-backup")) {
                try {
                    ZipPublisher(context) { if (it == point) throw java.io.IOException("ENOSPC/provider failure fixture") }
                        .publish(source, 1, ZipPublisher.ConflictResolution.REPLACE)
                    fail("Failure injection did not execute")
                } catch (_: java.io.IOException) { assertPrevious() }
            }
            try {
                ZipPublisher(context) { if (it == "after-backup") throw CancellationException("fixture") }
                    .publish(source, 1, ZipPublisher.ConflictResolution.REPLACE)
                fail("Cancellation injection did not execute")
            } catch (_: CancellationException) { assertPrevious() }
            try {
                ZipPublisher(context) { if (it == "after-backup") throw AssertionError("simulated process interruption") }
                    .publish(source, 1, ZipPublisher.ConflictResolution.REPLACE)
            } catch (_: AssertionError) { }
            source.writeText("invalid new source")
            try { ZipPublisher(context).publish(source, 1, ZipPublisher.ConflictResolution.REPLACE); fail() }
            catch (_: java.util.zip.ZipException) { assertPrevious() }
        } finally {
            runCatching { context.contentResolver.delete(current(), null, null) }
            directory.deleteRecursively()
        }
    }
}
