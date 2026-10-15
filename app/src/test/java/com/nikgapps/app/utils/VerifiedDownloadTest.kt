package com.nikgapps.app.utils

import com.nikgapps.app.utils.download.VerifiedDownload
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VerifiedDownloadTest {
    private fun archive(): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> zip.putNextEntry(ZipEntry("fixture")); zip.write(ByteArray(10000) { 1 }); zip.closeEntry() }
    }.toByteArray()
    private fun client(bytes: ByteArray, code: Int = 200) = OkHttpClient.Builder().addInterceptor { chain ->
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(bytes.toResponseBody("application/zip".toMediaType())).build()
    }.build()
    @Test fun truncatedInvalidOfflineAndUnavailableKeepPreviousOutput() = runBlocking {
        val dir = Files.createTempDirectory("download-fixture").toFile()
        try {
            val destination = dir.resolve("release.zip"); val old = archive(); destination.writeBytes(old)
            assertFalse(VerifiedDownload(client(archive())).download("https://fixture.invalid/release.zip", destination, expectedSize = 100000))
            assertFalse(VerifiedDownload(client("not a zip".toByteArray())).download("https://fixture.invalid/release.zip", destination))
            assertFalse(VerifiedDownload(client(old, 404)).download("https://fixture.invalid/release.zip", destination))
            val offline = OkHttpClient.Builder().addInterceptor { throw IOException("Offline fixture") }.build()
            assertFalse(VerifiedDownload(offline).download("https://fixture.invalid/release.zip", destination))
            assertArrayEquals(old, destination.readBytes())
            assertEquals(listOf("release.zip"), dir.list()!!.toList())
        } finally { dir.deleteRecursively() }
    }
    @Test fun invalidCacheIsReplacedOnlyByVerifiedRetry() = runBlocking {
        val dir = Files.createTempDirectory("download-retry").toFile()
        try {
            val destination = dir.resolve("release.zip"); destination.writeText("partial")
            assertTrue(VerifiedDownload(client(archive())).download("https://fixture.invalid/release.zip", destination))
            com.nikgapps.app.registry.SafeFilePublication.validZip(destination)
        } finally { dir.deleteRecursively() }
    }
    @Test fun cancellationCleansOnlyOwnStageAndPreservesDestination() = runBlocking {
        val dir = Files.createTempDirectory("download-cancel").toFile()
        try {
            val destination = dir.resolve("release.zip"); val old = archive(); destination.writeBytes(old)
            val job = launch {
                VerifiedDownload(client(old)).download("https://fixture.invalid/release.zip", destination,
                    progress = { throw CancellationException("fixture cancellation") })
            }
            job.join(); assertTrue(job.isCancelled)
            assertArrayEquals(old, destination.readBytes())
            assertEquals(1, dir.list()!!.size)
        } finally { dir.deleteRecursively() }
    }
}
