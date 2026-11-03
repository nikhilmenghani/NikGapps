package com.nikgapps.app.utils.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ReleaseMigrationTest {
    @Test fun stableUpdatesOnlyQueryReleaseRepository() = runBlocking {
        val calls = mutableListOf<String>()
        val release = VersionFetcher.resolveRelease(false) {
            calls += it
            """{"tag_name":"v0.81","prerelease":false}"""
        }!!
        assertEquals(listOf(VersionFetcher.RELEASE_REPOSITORY), calls)
        assertEquals("0.81", release.version)
        assertEquals("https://github.com/nikhilmenghani/NikGappsReleases/releases/download/v0.81/NikGapps-v0.81.apk", release.url)
    }
    @Test fun missingOrMalformedReleaseDoesNotFallBackToSource() = runBlocking {
        for (body in listOf(null, "not JSON", "{}")) {
            val calls = mutableListOf<String>()
            assertNull(VersionFetcher.resolveRelease(false) { calls += it; body })
            assertEquals(listOf(VersionFetcher.RELEASE_REPOSITORY), calls)
        }
    }
    @Test fun developmentUpdatesPreserveChannelAndFilename() = runBlocking {
        val calls = mutableListOf<String>()
        val release = VersionFetcher.resolveRelease(true) {
            calls += it
            """[{"tag_name":"v0.81","prerelease":false},{"tag_name":"dev-v0.80.21","prerelease":true}]"""
        }!!
        assertEquals(listOf(VersionFetcher.RELEASE_REPOSITORY), calls)
        assertEquals("0.80.21", release.version)
        assertEquals("https://github.com/nikhilmenghani/NikGappsReleases/releases/download/dev-v0.80.21/NikGapps-debug-dev-v0.80.21.apk", release.url)
    }
    @Test fun uncachedDownloadUrlUsesReleaseRepository() {
        assertTrue(VersionFetcher.downloadUrl("99.99.99").startsWith(
            "https://github.com/nikhilmenghani/NikGappsReleases/releases/download/"))
    }
    @Test fun draftsAndPrereleasesCannotBecomeStableUpdates() = runBlocking {
        assertNull(VersionFetcher.resolveRelease(false) { """{"tag_name":"v0.81","prerelease":true}""" })
        assertNull(VersionFetcher.resolveRelease(false) { """{"tag_name":"v0.81","draft":true}""" })
        assertEquals("Unknown", VersionFetcher.parseLatestDevelopmentVersion("""[{"tag_name":"dev-v0.80.21","draft":true,"prerelease":true}]"""))
    }
    @Test fun cancellationIsPropagated() {
        assertThrows(CancellationException::class.java) { runBlocking {
            VersionFetcher.resolveRelease(false) { throw CancellationException() }
        } }
    }
}
