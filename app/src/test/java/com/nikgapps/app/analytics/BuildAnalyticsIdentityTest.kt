package com.nikgapps.app.analytics

import org.junit.Assert.*
import org.junit.Test

class BuildAnalyticsIdentityTest {
    @Test fun capturedIdentitySurvivesLogoutAndAccountSwitch() {
        val identity = BuildAnalyticsIdentity("verified-user")
        val loggedOut = mapOf<String, Any>("account_mode" to "signed_out", "package_count" to 3)
        val result = buildEventProperties(loggedOut, identity)
        assertEquals("verified-user", result["github_username"])
        assertEquals("signed_in", result["account_mode"])
        assertEquals(3, result["package_count"])
        val switched = loggedOut + ("github_username" to "other-user")
        assertEquals("verified-user", buildEventProperties(switched, identity)["github_username"])
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsBlankVerifiedUsername() {
        BuildAnalyticsIdentity(" ")
    }
}
