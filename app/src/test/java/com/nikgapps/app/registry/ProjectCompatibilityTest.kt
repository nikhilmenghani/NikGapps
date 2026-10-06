package com.nikgapps.app.registry

import com.nikgapps.app.data.AndroidVersion
import com.nikgapps.app.data.Architecture
import com.nikgapps.app.data.BuildProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCompatibilityTest {
    private val metadata = RegistryMetadata(
        CatalogParser.parseCatalog(RegistryTestFixtures.catalog()),
        CatalogParser.parseAppSets(RegistryTestFixtures.appSets()),
        emptyMap(), null, null, false, 0L
    )

    @Test fun historicalCatalogEntryIsUnavailableWhenMissingFromReleaseAppSets() {
        val original = BuildProject(name = "Old", androidVersion = AndroidVersion.ANDROID_17,
            architecture = Architecture.ARM64,
            selectedAppIds = setOf("gms_core", "removed_package"),
            selectedPackageAppSets = mapOf("gms_core" to "core", "removed_package" to "core"),
            channelOverrides = mapOf("removed_package" to "beta"))
        assertEquals(setOf("removed_package"), unavailableProjectPackages(original, metadata))
        val copy = duplicateCurrentProject(original, metadata)
        assertEquals(setOf("gms_core"), copy.selectedAppIds)
        assertEquals(mapOf("gms_core" to "core"), copy.selectedPackageAppSets)
        assertTrue(copy.channelOverrides.isEmpty())
        assertEquals(setOf("gms_core", "removed_package"), original.selectedAppIds)
    }
}
