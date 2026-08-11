package com.nikgapps.app.utils.network

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionFetcherTest {
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
}
