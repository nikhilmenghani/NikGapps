package com.nikgapps.app.registry

import com.nikgapps.app.data.zipFilenameTimestamp
import java.time.LocalDate
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class BuildDatePrefsTest {
    @Test fun longerNamesAreEliteOnly() {
        assertEquals(20, com.nikgapps.app.data.projectNameLimit(false))
        assertEquals(25, com.nikgapps.app.data.projectNameLimit(true))
    }
    @Test fun currentDateIsEliteOnlyAndReleaseDateRemainsDefault() {
        val today = LocalDate.of(2026, 10, 10)
        assertNull(zipFilenameTimestamp(false, true, today))
        assertNull(zipFilenameTimestamp(true, false, today))
        assertEquals(Instant.parse("2026-10-10T00:00:00Z"), zipFilenameTimestamp(true, true, today))
    }
}
