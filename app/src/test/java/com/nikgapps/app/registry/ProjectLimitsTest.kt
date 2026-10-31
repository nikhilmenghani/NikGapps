package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectLimitsTest {
    @Test fun regularAccountsCanAddTheSixthButNotTheSeventhProject() {
        for (count in 0..5) {
            assertTrue(canCreateProject(count, false))
            requireProjectCapacity(count, false)
        }
        assertFalse(canCreateProject(6, false))
        assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(6, false) }
    }

    @Test fun existingOverLimitAccountsCannotAddButCanRecoverByDeleting() {
        for (count in listOf(7, 10, 100)) {
            assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(count, false) }
        }
        assertTrue(canCreateProject(5, false))
    }

    @Test fun eliteAccountsHaveNoProjectCountLimit() {
        for (count in listOf(0, 4, 5, 6, 100)) {
            assertTrue(canCreateProject(count, true))
            requireProjectCapacity(count, true)
        }
        assertTrue(AppFeatureCatalog.eliteFeatures.any { it.title == "Unlimited projects" })
    }

    @Test fun copiesAndImportedConfigsUseTheSameCountPolicy() {
        var count = 4
        repeat(2) { requireProjectCapacity(count, false); count++ }
        assertEquals(6, count)
        assertThrows(ProjectLimitReachedException::class.java) { requireProjectCapacity(count, false) }
    }
}
