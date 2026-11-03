package com.nikgapps.app.utils.network

import com.nikgapps.app.update.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChangelogStatusTest {
    @Test fun installedBadgeHandlesDebugSuffix() {
        assertEquals(ChangelogStatus.INSTALLED, changelogStatus("0.80.21", "0.80.21-debug", "0.80.21"))
    }
    @Test fun onlyUnpublishedFutureVersionIsUpcoming() {
        assertEquals(ChangelogStatus.UPCOMING, changelogStatus("0.80.22", "0.80.20", "0.80.21"))
        assertEquals(ChangelogStatus.RELEASED, changelogStatus("0.80.21", "0.80.20", "0.80.21"))
        assertEquals(ChangelogStatus.RELEASED, changelogStatus("0.80.19", "0.80.20", "0.80.21"))
    }
    @Test fun unknownPublicationStateDoesNotClaimUpcoming() {
        for (published in listOf(null, "Unknown")) {
            assertEquals(ChangelogStatus.RELEASED, changelogStatus("0.80.22", "0.80.20", published))
            assertEquals(ChangelogStatus.INSTALLED, changelogStatus("0.80.20", "0.80.20-debug", published))
        }
    }
    @Test fun publicationLookupUsesReleaseRepository() = runBlocking {
        val release = VersionFetcher.resolveRelease(true) { repo ->
            assertEquals(VersionFetcher.RELEASE_REPOSITORY, repo)
            val version = "0.80.22"
            """[{"tag_name":"dev-v$version","prerelease":true}]"""
        }
        assertEquals("0.80.22", release?.version)
    }
}
