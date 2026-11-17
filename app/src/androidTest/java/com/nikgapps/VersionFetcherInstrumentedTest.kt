package com.nikgapps

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.UpdatePrefs
import com.nikgapps.app.data.dataStore
import com.nikgapps.app.update.ChangelogStatus
import com.nikgapps.app.update.changelogStatus
import com.nikgapps.app.utils.network.VersionFetcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionFetcherInstrumentedTest {
    @Test fun savedSignInTokenAndPersistedChannelCacheSurviveFailedRefresh() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val oldToken = GithubPrefs.token
        val oldDevelopment = UpdatePrefs.lastSuccessfulDevelopmentRelease
        val oldStable = UpdatePrefs.lastSuccessfulStableRelease
        try {
            // Both the OAuth device flow and manual token entry save to this preference.
            GithubPrefs.token = "gho_device_flow_test_fixture"
            VersionFetcher.fetchRelease(true, GithubPrefs.token,
                { UpdatePrefs.lastSuccessfulDevelopmentRelease },
                { UpdatePrefs.lastSuccessfulDevelopmentRelease = it }) { request ->
                assertEquals("Bearer gho_device_flow_test_fixture", request.header("Authorization"))
                """[{"tag_name":"dev-v0.80.22","prerelease":true}]"""
            }
            VersionFetcher.fetchRelease(false, GithubPrefs.token,
                { UpdatePrefs.lastSuccessfulStableRelease },
                { UpdatePrefs.lastSuccessfulStableRelease = it }) {
                """{"tag_name":"v0.81","prerelease":false}"""
            }
            val persisted = context.dataStore.data.first()
            val developmentKey = stringPreferencesKey("nikgapps_releases_last_development_version")
            val stableKey = stringPreferencesKey("nikgapps_releases_last_stable_version")
            assertEquals("0.80.22", persisted[developmentKey])
            assertEquals("0.81", persisted[stableKey])
            assertEquals("0.80.22", VersionFetcher.lastKnownVersion())

            // Read the persisted value, as initialization after a restart does.
            val retained = VersionFetcher.fetchRelease(true, GithubPrefs.token,
                { persisted[developmentKey] }, { error("Failed request must not replace the cache") }) { null }
            assertEquals("0.80.22", retained?.version)
            assertEquals(ChangelogStatus.UPCOMING, changelogStatus("0.81", "0.80.22-debug", retained?.version))
            assertEquals("0.81", context.dataStore.data.first()[stableKey])
        } finally {
            GithubPrefs.token = oldToken
            UpdatePrefs.lastSuccessfulDevelopmentRelease = oldDevelopment
            UpdatePrefs.lastSuccessfulStableRelease = oldStable
        }
    }
}
