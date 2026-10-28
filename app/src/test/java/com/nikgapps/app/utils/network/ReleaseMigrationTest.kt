package com.nikgapps.app.utils.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ReleaseMigrationTest {
    private fun stable(version: String) = """{"tag_name":"v$version","prerelease":false}"""
    @Test fun newerSourceReleaseKeepsExistingUpdatePath() = runBlocking {
        val calls = mutableListOf<String>()
        val release = VersionFetcher.resolveRelease("0.80", false) { calls += it; stable("0.81") }!!
        assertEquals(listOf(VersionFetcher.SOURCE_REPOSITORY), calls)
        assertTrue(release.url.contains("/NikGapps/releases/"))
    }
    @Test fun unchangedSourceFallsBackToNewPublicVersionAndMatchingArtifact() = runBlocking {
        val release = VersionFetcher.resolveRelease("0.80", false) {
            stable(if (it == VersionFetcher.SOURCE_REPOSITORY) "0.80" else "0.81")
        }!!
        assertEquals("0.81", release.version)
        assertEquals("https://github.com/nikhilmenghani/NikGappsReleases/releases/download/v0.81/NikGapps-v0.81.apk", release.url)
    }
    @Test fun privateMissingOrMalformedSourceFallsBack() = runBlocking {
        for (source in listOf(null, "not JSON", "{}")) {
            val release = VersionFetcher.resolveRelease("0.80", false) {
                if (it == VersionFetcher.SOURCE_REPOSITORY) source else stable("0.81")
            }!!
            assertTrue(release.url.contains("/NikGappsReleases/"))
        }
    }
    @Test fun offlineFallbackKeepsCurrentSourceWithoutInventingAnUpdate() = runBlocking {
        val release = VersionFetcher.resolveRelease("0.80", false) {
            if (it == VersionFetcher.SOURCE_REPOSITORY) stable("0.80") else null
        }!!
        assertFalse(VersionFetcher.isNewer(release.version, "0.80"))
        assertNull(VersionFetcher.resolveRelease("0.80", false) { null })
    }
    @Test fun developmentFallbackPreservesChannelAndFilename() = runBlocking {
        val release = VersionFetcher.resolveRelease("0.80.20-debug", true) {
            if (it == VersionFetcher.SOURCE_REPOSITORY) "[]" else
                """[{"tag_name":"v0.81","prerelease":false},{"tag_name":"dev-v0.80.21","prerelease":true}]"""
        }!!
        assertEquals("0.80.21", release.version)
        assertTrue(release.url.endsWith("/dev-v0.80.21/NikGapps-debug-dev-v0.80.21.apk"))
    }
    @Test fun draftsAndPrereleasesCannotBecomeStableUpdates() = runBlocking {
        assertNull(VersionFetcher.resolveRelease("0.80", false) { """{"tag_name":"v0.81","prerelease":true}""" })
        assertEquals("Unknown", VersionFetcher.parseLatestDevelopmentVersion("""[{"tag_name":"dev-v0.80.21","draft":true,"prerelease":true}]"""))
    }
    @Test fun cancellationIsPropagated() {
        assertThrows(CancellationException::class.java) { runBlocking {
            VersionFetcher.resolveRelease("0.80", false) { throw CancellationException() }
        } }
    }
}
