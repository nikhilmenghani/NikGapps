package com.nikgapps.app.utils.network

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionFetcherTest {
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
