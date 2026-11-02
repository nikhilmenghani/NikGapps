package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectLimitsTest {
    @Test fun regularAccountsCanAddTheFifthButNotTheSixthProject() {
        for (count in 0..4) {
            assertTrue(canCreateProject(count, false))
            requireProjectCapacity(count, false)
        }
        assertFalse(canCreateProject(5, false))
        assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(5, false) }
    }

    @Test fun existingOverLimitAccountsCannotAddButCanRecoverByDeleting() {
        for (count in listOf(6, 10, 100)) {
            assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(count, false) }
        }
        assertTrue(canCreateProject(4, false))
    }

    @Test fun eliteAccountsHaveNoProjectCountLimit() {
        for (count in listOf(0, 4, 5, 6, 100)) {
            assertTrue(canCreateProject(count, true))
            requireProjectCapacity(count, true)
        }
        assertTrue(AppFeatureCatalog.eliteFeatures.any { it.title == "Unlimited projects" })
    }

    @Test fun copiesAndImportedConfigsUseTheSameCountPolicy() {
        var count = 3
        repeat(2) { requireProjectCapacity(count, false); count++ }
        assertEquals(5, count)
        assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(count, false) }
    }
}
