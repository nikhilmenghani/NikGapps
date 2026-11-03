package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class BuildCreatorTest {
    @Test fun usesGitHubUsernameAndGuestFallback() {
        assertEquals("Created by nikhilmenghani", buildCreatorLabel("nikhilmenghani"))
        assertEquals("Created by Guest", buildCreatorLabel(null))
        assertEquals("Created by Guest", buildCreatorLabel(""))
    }
    @Test fun rejectsMultilineAndInvalidCreatorNames() {
        for (value in listOf("name\nother", "../../name", "x".repeat(40))) {
            assertThrows(IllegalArgumentException::class.java) { buildCreatorLabel(value) }
        }
    }
}
