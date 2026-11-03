package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectBackupReviewTest {
    private fun metadata(): RegistryMetadata {
        val catalog = CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).let { parsed ->
            parsed.copy(packages = parsed.packages.map { pkg -> pkg.copy(versions = pkg.versions.mapValues {
                it.value.copy(android = it.value.android.copy(maxApi = null))
            }) })
        }
        val sets = CatalogParser.parseAppSets(RegistryTestFixtures.appSets())
        val release = CatalogRelease(1, "current", "2026-10-11T12:00:00Z", "17", 37,
            "arm64-v8a", "stable", "test", emptyMap(), sets.appSets)
        return RegistryMetadata(catalog, sets, mapOf(RegistryZipAssembler.CONFIG_TEMPLATE to
            BuilderAsset("https://example.com/config", "hash", 1, "41", "17", packageDefinitions = mapOf("gms_core" to "same"))),
            null, release, false, 0)
    }
    private fun project() = BuildProject(name = "Test", androidVersion = AndroidVersion.ANDROID_17,
        architecture = Architecture.ARM64, selectedAppIds = setOf("gms_core"),
        selectedPackageAppSets = mapOf("gms_core" to "core"), configVersion = "40", packageDefinitions = mapOf("gms_core" to "same"))

    @Test fun keepsCompatibleUpdatesButSkipsChangedDefinitionsAndMissingPackages() {
        val valid = project()
        val changed = project().copy(packageDefinitions = mapOf("gms_core" to "old"))
        val retired = project().copy(selectedAppIds = setOf("retired"))
        val plan = planBackupRestore(listOf(valid, changed, retired), mapOf(valid.backupTarget() to metadata()))
        assertEquals(listOf(valid), plan.compatible)
        assertEquals(listOf(changed, retired), plan.outdated)
        // Export still retains the outdated definitions for archival/recovery.
        val file = ProjectBackups.export(listOf(valid, changed, retired), BackupSource("0.80.22", 1, "release"))
        assertEquals(3, ProjectBackups.read(file).projects.size)
    }
    @Test fun staleOrUnavailableMetadataStopsImportInsteadOfGuessing() {
        val p = project()
        assertThrows(IllegalStateException::class.java) { planBackupRestore(listOf(p), emptyMap()) }
        assertThrows(IllegalStateException::class.java) {
            planBackupRestore(listOf(p), mapOf(p.backupTarget() to metadata().copy(fromCache = true)))
        }
    }
    @Test fun wrongPlatformIsOutdatedAndAllOutdatedDoesNotBecomeEmptyReplacement() {
        val p = project().copy(androidVersion = AndroidVersion.ANDROID_16)
        val plan = planBackupRestore(listOf(p), mapOf(p.backupTarget() to metadata()))
        assertEquals(listOf(p), plan.outdated)
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.replacement(plan.compatible, true) }
    }
}
