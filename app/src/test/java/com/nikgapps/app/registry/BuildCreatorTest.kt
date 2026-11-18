package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class BuildCreatorTest {
    @Test fun usesGitHubUsername() {
        assertEquals("Created by nikhilmenghani", buildCreatorLabel("nikhilmenghani"))
    }
    @Test fun rejectsMultilineAndInvalidCreatorNames() {
        for (value in listOf(null, "", " ", "name\nother", "../../name", "x".repeat(40))) {
            assertThrows(IllegalArgumentException::class.java) { buildCreatorLabel(value) }
        }
    }
}
