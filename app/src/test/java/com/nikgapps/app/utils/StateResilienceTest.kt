package com.nikgapps.app.utils

import com.nikgapps.app.data.ExplicitRelease
import com.nikgapps.app.data.recoverRecords
import com.nikgapps.app.security.RelockPolicy
import org.junit.Assert.*
import org.junit.Test

class StateResilienceTest {
    @Test fun appLockRelocksAfterBackgroundTimeoutSessionTimeoutAndProcessRecreation() {
        val policy = RelockPolicy()
        assertFalse(policy.unlocked(0)); policy.authenticate(100)
        policy.background(200); assertTrue(policy.resume(500))
        policy.background(1000); assertFalse(policy.resume(31000))
        policy.authenticate(40000); assertFalse(policy.unlocked(340000))
        assertFalse(RelockPolicy().unlocked(40000))
        assertFalse(policy.unlocked(39999))
    }
    @Test fun malformedProjectDoesNotHideValidProjects() {
        val errors = mutableListOf<String>()
        val values = recoverRecords(listOf("1", "broken", "3"), { it.toInt() }, { index, _ -> errors += index })
        assertEquals(listOf(1, 3), values); assertEquals(listOf("broken"), errors)
    }
    @Test fun releaseSelectionRejectsEmptyMalformedInsecureAndCredentialUrls() {
        listOf("", "garbage", "http://example.com/release.zip", "https://user:pass@example.com/release.zip", "https://example.com/latest").forEach {
            assertThrows(IllegalArgumentException::class.java) { ExplicitRelease.parse(it) }
        }
        val release = ExplicitRelease.parse("https://example.com/release.zip?download=1")
        assertEquals("release.zip", release.fileName)
        assertEquals("https://example.com/release.zip?download=1", release.url)
    }
}
