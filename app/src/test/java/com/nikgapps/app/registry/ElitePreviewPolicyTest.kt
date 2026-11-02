package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ElitePreviewPolicyTest {
    @Test fun hiddenDeveloperSettingsUseDefaultsEvenWithPersistedOverrides() {
        for (defaultValue in listOf(false, true)) {
            for (stored in listOf(false, true)) {
                assertEquals(defaultValue, developerOptionValue(false, stored, defaultValue))
                assertEquals(stored, developerOptionValue(true, stored, defaultValue))
            }
        }
    }
    @Test fun disabledPreviewPreservesNormalDetection() {
        assertFalse(nonElitePreviewEnabled(false, false))
        assertFalse(nonElitePreviewEnabled(true, false))
    }
    @Test fun previewRequiresDeveloperOptions() {
        assertTrue(nonElitePreviewEnabled(true, true))
        assertFalse(nonElitePreviewEnabled(false, true))
    }
    @Test fun previewUsesRegularProjectAndNameLimitsWithoutChangingProjects() {
        val effectiveElite = !nonElitePreviewEnabled(true, true)
        assertFalse(canCreateProject(5, effectiveElite))
        assertEquals(20, projectNameLimit(effectiveElite))
        val saved = BuildProject(name = "Existing long Elite name", androidVersion = AndroidVersion.ANDROID_17,
            architecture = Architecture.ARM64)
        assertEquals("Existing long Elite name", saved.name)
    }
    @Test fun disablingDeveloperOptionsRestoresRealMembershipNotAnEliteGrant() {
        for (verified in listOf(false, true)) {
            assertEquals(verified, verified && !nonElitePreviewEnabled(false, true))
            assertEquals(verified, verified && !nonElitePreviewEnabled(true, false))
            assertFalse(verified && !nonElitePreviewEnabled(true, true))
        }
    }
}
