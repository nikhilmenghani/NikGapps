package com.nikgapps.app.registry

import com.nikgapps.app.data.AndroidVersion
import com.nikgapps.app.data.Architecture
import com.nikgapps.app.data.BuildProject
import org.junit.Assert.*
import org.junit.Test

class StockAppRemovalsTest {
    private val project = BuildProject(name = "Test", androidVersion = AndroidVersion.ANDROID_17,
        architecture = Architecture.ARM64)
    private val base = CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).packages.first()
    private fun version(vararg removals: String) = base.versions.getValue("s").copy(
        install = InstallMetadata("nikgapps-package-v1", "GoogleCalendar", "GoogleCalendar", 1,
            removals.toList(), emptyList(), emptyList(), false, "09"))
    private val pkg = base.copy(versions = mapOf("s" to version("Calendar"),
        "b" to version("Etar"), "c" to version("SimpleCalendar")))

    @Test fun stockPreferenceSelectsPackageAndPreservesOtherProjectChoices() {
        val original = project.copy(selectedAppIds = setOf("youtube"),
            selectedPackageAppSets = mapOf("youtube" to "youtube"),
            keepAospCounterparts = setOf("other"))
        val kept = original.withStockAppPreference("calendar", "calendar", true)
        assertEquals(setOf("youtube", "calendar"), kept.selectedAppIds)
        assertEquals(mapOf("youtube" to "youtube", "calendar" to "calendar"), kept.selectedPackageAppSets)
        assertEquals(setOf("other", "calendar"), kept.keepAospCounterparts)
        assertEquals(setOf("youtube"), original.selectedAppIds)
        val normalRemoval = kept.withStockAppPreference("calendar", "calendar", false)
        assertEquals(kept.selectedAppIds, normalRemoval.selectedAppIds)
        assertEquals(setOf("other"), normalRemoval.keepAospCounterparts)
    }

    @Test fun stockPreferenceRespectsMutuallyExclusiveAppSets() {
        val original = project.copy(selectedAppIds = setOf("go", "youtube"),
            selectedPackageAppSets = mapOf("go" to "core_go", "youtube" to "youtube"))
        val selected = original.withStockAppPreference("gms", "core", true)
        assertEquals(setOf("gms", "youtube"), selected.selectedAppIds)
        assertFalse("go" in selected.selectedPackageAppSets)
        assertTrue("gms" in selected.keepAospCounterparts)
    }

    @Test fun projectChannelAndOverrideSelectOnlyTheirOwnRules() {
        assertEquals(listOf("Calendar"), pkg.stockAppRemovals(project))
        assertEquals(listOf("Etar"), pkg.stockAppRemovals(project.copy(
            channelOverrides = mapOf(pkg.id to "beta"))))
    }

    @Test fun releasePinTakesPrecedenceOverProjectChannels() {
        val release = CatalogParser.parseRelease(RegistryTestFixtures.release())
        assertEquals(listOf("SimpleCalendar"), pkg.stockAppRemovals(project.copy(
            channelOverrides = mapOf(pkg.id to "beta")), release))
        assertEquals(listOf("Calendar"), pkg.stockAppRemovals(project,
            release.copy(packages = emptyMap())))
    }

    @Test fun unavailableVersionOrMetadataNeverUsesAnUnrelatedVersion() {
        val release = CatalogParser.parseRelease(RegistryTestFixtures.release())
        assertTrue(pkg.stockAppRemovals(project, release.copy(packages = mapOf(pkg.id to "missing"))).isEmpty())
        assertTrue(pkg.stockAppRemovals(project.copy(defaultChannel = "missing")).isEmpty())
        assertTrue(pkg.copy(versions = mapOf("s" to base.versions.getValue("s")))
            .stockAppRemovals(project).isEmpty())
        assertTrue(pkg.copy(versions = mapOf("s" to version())).stockAppRemovals(project).isEmpty())
    }

    @Test fun removesBlankAndDuplicateRulesButPreservesCaseVariantsAndOrder() {
        val messaging = pkg.copy(versions = mapOf("s" to version("Messaging", "", " ",
            "messaging", "Messaging", "Mms")))
        assertEquals(listOf("Messaging", "messaging", "Mms"), messaging.stockAppRemovals(project))
    }

    @Test fun keptPackagesProtectEveryMetadataCounterpartIncludingFullPaths() {
        val photos = pkg.copy(id = "new_photos_package", name = "Google Photos", versions = mapOf(
            "s" to version("Gallery", "/product/priv-app/GalleryX/GalleryX.apk"),
            "b" to version("Glimpse")))
        val conflicts = DebloaterConfig.conflicts(listOf(photos), setOf(photos.id))
        for (target in listOf("Gallery", "GalleryX", "Glimpse", "/system/app/GalleryX/GalleryX.apk")) {
            assertThrows(target, IllegalArgumentException::class.java) {
                DebloaterConfig.validate(listOf(target), conflicts)
            }
        }
        assertEquals(listOf("OtherGallery"), DebloaterConfig.validate(listOf("OtherGallery"), conflicts))
        assertTrue("Google Photos" in conflicts)
        assertTrue(photos.id in conflicts)
        assertTrue("test.stable" in conflicts)
        assertTrue("Test" in conflicts)
    }

    @Test fun stockRulesAreNotProtectedWithoutKeepPreference() {
        val conflicts = DebloaterConfig.conflicts(listOf(pkg))
        assertEquals(listOf("Calendar"), DebloaterConfig.validate(listOf("Calendar"), conflicts))
        // An ID alone does not imply a stock counterpart; installer metadata is authoritative.
        assertFalse("DeskClock" in DebloaterConfig.conflicts(listOf(base.copy(id = "google_clock")),
            setOf("google_clock")))
    }
}
