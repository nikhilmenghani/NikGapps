package com.nikgapps.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildQuotaRepositoryTest {
    private val window = BuildQuotaRepository.DEFAULT_WINDOW_MILLIS

    @Test fun firstEliteResetIsReady() {
        assertTrue(eliteResetReady(now = window, lastResetAt = 0L, windowMillis = window))
    }

    @Test fun eliteResetHasSixHourCooldown() {
        val firstReset = 1_000L
        assertFalse(eliteResetReady(firstReset + window - 1L, firstReset, window))
        assertTrue(eliteResetReady(firstReset + window, firstReset, window))
        assertFalse(eliteResetReady(firstReset - 1L, firstReset, window))
    }

    @Test fun resetCarriesRemainingBuildsAndAddsSix() {
        assertEquals(6, eliteWindowLimit(0))
        assertEquals(8, eliteWindowLimit(2))
        assertEquals(12, eliteWindowLimit(6))
    }
}
