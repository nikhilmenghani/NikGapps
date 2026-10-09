package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectSelectionsTest {
    private val project = BuildProject(name = "Test", androidVersion = AndroidVersion.ANDROID_17,
        architecture = Architecture.ARM64)

    @Test fun sharedPackagesDoNotSelectBothAppSets() {
        val core = selectProjectPackages(project, setOf("shared", "core_only"), true, "core")
        assertTrue(isSelectedInAppSet(core, "shared", "core"))
        assertFalse(isSelectedInAppSet(core, "shared", "core_go"))
        val go = selectProjectPackages(core, setOf("shared", "go_only"), true, "core_go")
        assertTrue(isSelectedInAppSet(go, "shared", "core_go"))
        assertFalse(isSelectedInAppSet(go, "shared", "core"))
        val cleared = selectProjectPackages(go, setOf("shared", "core_only"), false, "core")
        assertEquals(setOf("shared", "go_only"), cleared.selectedAppIds)
        assertTrue(isSelectedInAppSet(cleared, "shared", "core_go"))
    }

    @Test fun packageViewKeepsExistingOwner() {
        val core = selectProjectPackages(project, setOf("shared"), true, "core")
        val selected = selectProjectPackages(core, setOf("shared"), true,
            defaultOwners = mapOf("shared" to "core_go"))
        assertEquals("core", selected.selectedPackageAppSets["shared"])
    }

    @Test fun wizardGroupsAreExclusiveDespiteDifferentPackageIds() {
        val setup = selectProjectPackages(project, setOf("setup", "setup_extra"), true, "setup_wizard")
        val pixel = selectProjectPackages(setup, setOf("pixel"), true, "pixel_setup_wizard")
        assertEquals(setOf("pixel"), pixel.selectedAppIds)
        val back = selectProjectPackages(pixel, setOf("setup"), true,
            defaultOwners = mapOf("setup" to "setup_wizard"))
        assertEquals(setOf("setup"), back.selectedAppIds)
        assertFalse(back.selectedPackageAppSets.containsKey("pixel"))
    }

    @Test fun selectAllKeepsAnExistingWizardChoice() {
        val pixel = selectProjectPackages(project, setOf("pixel"), true, "pixel_setup_wizard")
        val all = selectProjectPackages(pixel, setOf("pixel", "setup", "clock"), true,
            defaultOwners = mapOf("pixel" to "pixel_setup_wizard", "setup" to "setup_wizard", "clock" to "clock"))
        assertEquals(setOf("pixel", "clock"), all.selectedAppIds)
    }

    @Test fun legacyConflictingWizardSelectionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            requireCompatibleAppSets(listOf("core", "setup_wizard", "pixel_setup_wizard"))
        }
        requireCompatibleAppSets(listOf("core", "pixel_setup_wizard"))
    }

    @Test fun selectAllUsesCoreAndSetupWizardEvenAfterAlternateChoices() {
        fun set(id: String) = CatalogAppSet(id, id, emptyList(), emptyList(), emptyMap())
        val memberships = mapOf(
            "shared" to listOf(set("core_go"), set("core")),
            "go_only" to listOf(set("core_go")),
            "setup" to listOf(set("setup_wizard")),
            "pixel" to listOf(set("pixel_setup_wizard")),
            "clock" to listOf(set("clock")))
        val go = selectProjectPackages(project, setOf("shared", "go_only"), true, "core_go")
        val pixel = selectProjectPackages(go, setOf("pixel"), true, "pixel_setup_wizard")
        val defaults = defaultOwnersForSelectAll(memberships)
        val all = selectProjectPackages(pixel, defaults.keys, true, defaultOwners = defaults, useDefaultChoices = true)
        assertEquals(setOf("shared", "setup", "clock"), all.selectedAppIds)
        assertEquals("core", all.selectedPackageAppSets["shared"])
        assertEquals("setup_wizard", all.selectedPackageAppSets["setup"])
        requireCompatibleAppSets(all.selectedPackageAppSets.values)
    }
}
