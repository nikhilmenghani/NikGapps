package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class GmsCoreMigrationTest {
    private fun metadata(android: String = "17", version: String = "42"): RegistryMetadata {
        val old = CatalogParser.parseCatalog(RegistryTestFixtures.catalog())
        val gms = old.packages.first { it.id == "gms_core" }
        val packages = listOf(gms.copy(id = "gms_core_android17", dependencies = emptyList()),
            gms.copy(id = "gms_core_go_android17", dependencies = emptyList()))
        val sets = AppSetCatalog(1, listOf(
            CatalogAppSet("core", "Core", listOf(packages[0].id), listOf(packages[0].id), mapOf(packages[0].id to "GmsCore")),
            CatalogAppSet("core_go", "CoreGo", listOf(packages[1].id), listOf(packages[1].id), mapOf(packages[1].id to "GmsCore"))))
        val release = CatalogRelease(1, "new", "2026-11-09T12:00:00Z", android, 37, "arm64-v8a", "stable", "test",
            emptyMap(), sets.appSets)
        return RegistryMetadata(old.copy(packages = packages), sets, mapOf(RegistryZipAssembler.CONFIG_TEMPLATE to
            BuilderAsset("https://example.test/config", "hash", 1, version, android,
                packageDefinitions = packages.associate { it.id to "merged" })), null, release, false, 0)
    }
    private fun project(owner: String) = BuildProject(name = "Migration", androidVersion = AndroidVersion.ANDROID_17,
        architecture = Architecture.ARM64, selectedAppSetId = owner, selectedAppIds = setOf("gms_core"),
        selectedPackageAppSets = mapOf("gms_core" to owner), configVersion = "41", createdAt = 0,
        createdReleaseId = "old", packageDefinitions = mapOf("gms_core" to "legacy"),
        keepAospCounterparts = setOf("gms_core"), installerSettings = mapOf("Mode" to "install"))

    @Test fun coreAndGoMapToDistinctArtifactsAndPreserveSettings() {
        for ((owner, id) in mapOf("core" to "gms_core_android17", "core_go" to "gms_core_go_android17")) {
            val old = project(owner)
            val migrated = old.withCurrentConfig(metadata())
            assertEquals(setOf(id), migrated.selectedAppIds)
            assertEquals(owner, migrated.selectedPackageAppSets[id])
            assertEquals(setOf(id), migrated.keepAospCounterparts)
            assertEquals(old.installerSettings, migrated.installerSettings)
            assertEquals("42", migrated.configVersion)
            assertEquals(setOf("gms_core"), old.selectedAppIds)
            assertEquals(ProjectUpdateKind.AVAILABLE, projectUpdate(old, metadata()).kind)
        }
    }
    @Test fun legacyPlatformsAndPinnedOldReleasesDoNotMigrate() {
        val old = project("core")
        assertEquals(old, old.withAndroid17GmsCore(metadata(version = "41")))
        assertEquals(old, old.withAndroid17GmsCore(metadata(android = "16")))
        val sixteen = old.copy(androidVersion = AndroidVersion.ANDROID_16)
        assertEquals(sixteen, sixteen.withAndroid17GmsCore(metadata()))
    }
    @Test fun duplicatedProjectKeepsGmsCoreSelection() {
        assertEquals(setOf("gms_core_go_android17"), duplicateCurrentProject(project("core_go"), metadata()).selectedAppIds)
    }
}
