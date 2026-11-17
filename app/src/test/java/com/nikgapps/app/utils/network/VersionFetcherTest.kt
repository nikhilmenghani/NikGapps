package com.nikgapps.app.utils.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import java.io.IOException
import com.nikgapps.app.update.ChangelogStatus
import com.nikgapps.app.update.changelogStatus

class VersionFetcherTest {
    @Test fun `release checks authenticate both OAuth sign-in and manually supplied tokens`() {
        for (token in listOf("gho_example_oauth", "github_pat_example_manual")) {
            val request = VersionFetcher.releaseRequest(true, token)
            assertEquals("Bearer $token", request.header("Authorization"))
            assertEquals("api.github.com", request.url.host)
            assertEquals("/repos/nikhilmenghani/NikGappsReleases/releases", request.url.encodedPath)
            assertEquals("100", request.url.queryParameter("per_page"))
        }
        val stable = VersionFetcher.releaseRequest(false, "token")
        assertEquals("/repos/nikhilmenghani/NikGappsReleases/releases/latest", stable.url.encodedPath)
    }

    @Test fun `signed-out release checks omit authorization`() {
        for (token in listOf("", "  ")) {
            assertNull(VersionFetcher.releaseRequest(true, token).header("Authorization"))
        }
    }

    @Test fun `failed lookups retain last success and upcoming badge for both channels`() = runBlocking {
        for (development in listOf(true, false)) {
            val expected = if (development) "0.80.22" else "0.80"
            val response = if (development) """[{"tag_name":"dev-v$expected","prerelease":true}]"""
                else """{"tag_name":"v$expected","prerelease":false}"""
            var saved: String? = null
            VersionFetcher.fetchRelease(development, "gho_example", { saved }, { saved = it }) { request ->
                assertEquals("Bearer gho_example", request.header("Authorization"))
                response
            }
            assertEquals(expected, saved)
            for (failure in listOf("http403", "http500", "offline", "invalidJson", "emptyCatalog")) {
                val result = VersionFetcher.fetchRelease(development, "gho_example", { saved },
                    { fail("A failed lookup must not overwrite the successful version") }) {
                    when (failure) {
                        "http403", "http500" -> null
                        "offline" -> throw IOException("Offline")
                        "emptyCatalog" -> if (development) "[]" else "{}"
                        else -> "not JSON"
                    }
                }
                assertEquals(expected, result?.version)
                assertEquals(expected, saved)
                assertEquals(ChangelogStatus.UPCOMING, changelogStatus("0.81", "0.80.22-debug", result?.version))
                assertEquals(VersionFetcher.artifactUrl(VersionFetcher.RELEASE_REPOSITORY, expected, development), result?.url)
            }
        }
    }

    @Test fun `new successful lookup replaces cache and updates publication status`() = runBlocking {
        var saved: String? = "0.80.22"
        val result = VersionFetcher.fetchRelease(true, "token", { saved }, { saved = it }) {
            """[{"tag_name":"dev-v0.81.0","prerelease":true}]"""
        }
        assertEquals("0.81.0", saved)
        assertEquals(ChangelogStatus.RELEASED, changelogStatus("0.81", "0.80.22-debug", result?.version))
    }

    @Test fun `first failed lookup has unknown publication state`() = runBlocking {
        val result = VersionFetcher.fetchRelease(true, "token", { null }, { fail("No successful version") }) { null }
        assertNull(result)
        assertEquals(ChangelogStatus.UNKNOWN, changelogStatus("0.81", "0.80.22-debug", result?.version))
    }

    @Test fun `invalid cached versions are not used for release status or downloads`() = runBlocking {
        for (version in listOf("", "Unknown", "bad-value", "0.81", "99.0.0/extra")) {
            val result = VersionFetcher.fetchRelease(true, "token", { version }, { fail("Failed lookup") }) { null }
            assertNull(result)
        }
    }

    @Test fun `cancellation propagates without consulting or overwriting cache`() = runBlocking {
        var readCache = false
        try {
            VersionFetcher.fetchRelease(true, "token", { readCache = true; "0.80.22" }, { fail("Cancelled lookup") }) {
                throw CancellationException("Cancelled")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertFalse(readCache)
        }
    }

    @Test
    fun `debug suffix does not make installed version look outdated`() {
        assertEquals(false, VersionFetcher.isNewer("0.80.4", "0.80.4-debug"))
        assertEquals(true, VersionFetcher.isNewer("0.80.5", "0.80.4-debug"))
    }

    @Test
    fun `uses release tag instead of display name`() {
        val release = """{"tag_name":"v1.2.3","name":"NikGapps release 1.2.3"}"""

        assertEquals("1.2.3", VersionFetcher.parseReleaseVersion(release))
    }

    @Test
    fun `falls back to release name when tag is absent`() {
        val release = """{"name":"v1.2.3"}"""

        assertEquals("1.2.3", VersionFetcher.parseReleaseVersion(release))
    }

    @Test
    fun `development channel selects highest valid prerelease`() {
        val releases = """[
            {"tag_name":"v0.81","prerelease":false},
            {"tag_name":"dev-v0.80.2","prerelease":true},
            {"tag_name":"dev-v0.80.12","prerelease":true},
            {"tag_name":"other-v9.9.9","prerelease":true}
        ]"""

        assertEquals("0.80.12", VersionFetcher.parseLatestDevelopmentVersion(releases))
    }
}
