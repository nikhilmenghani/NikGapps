package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class ProjectUpdatesTest {
    private fun metadata(id: String = "new", android: String = "17", version: String = "41", digest: String = "same"): RegistryMetadata {
        val catalog = CatalogParser.parseCatalog(RegistryTestFixtures.catalog())
        val sets = CatalogParser.parseAppSets(RegistryTestFixtures.appSets())
        val release = CatalogRelease(1, id, "2026-10-11T12:00:00Z", android, 37, "arm64-v8a", "stable", "test", emptyMap(), sets.appSets)
        return RegistryMetadata(catalog, sets, mapOf(RegistryZipAssembler.CONFIG_TEMPLATE to
            BuilderAsset("https://example.com/config", "hash", 1, version, android,
                packageDefinitions = mapOf("gms_core" to digest))), null, release, false, 0)
    }
    private fun project() = BuildProject(name="Example", androidVersion=AndroidVersion.ANDROID_17,
        architecture=Architecture.ARM64, createdAt=0, createdReleaseId="old", configVersion="40",
        selectedAppIds=setOf("gms_core"), selectedPackageAppSets=mapOf("gms_core" to "core"),
        packageDefinitions=mapOf("gms_core" to "same"))

    @Test fun compatibleReleaseOffersBuildAndSuccessClearsIt() {
        val metadata = metadata()
        assertEquals(ProjectUpdateKind.AVAILABLE, projectUpdate(project(), metadata).kind)
        val built = project().afterSuccessfulBuild(metadata)
        assertEquals("41", built.configVersion)
        assertEquals(ProjectUpdateKind.CURRENT, projectUpdate(built, metadata).kind)
    }
    @Test fun dismissalAppliesOnlyToOneRelease() {
        val dismissed = project().copy(dismissedReleaseId="new")
        assertEquals(ProjectUpdateKind.CURRENT, projectUpdate(dismissed, metadata()).kind)
        assertEquals(ProjectUpdateKind.AVAILABLE, projectUpdate(dismissed, metadata(id="next")).kind)
        assertEquals("old", dismissed.createdReleaseId)
    }
    @Test fun changedDefinitionCannotBeDismissedOrBuilt() {
        val dismissed = project().copy(dismissedReleaseId="new")
        assertEquals(ProjectUpdateKind.INCOMPATIBLE, projectUpdate(dismissed, metadata(digest="changed")).kind)
    }
    @Test fun anotherAndroidReleaseDoesNotHighlightProject() {
        assertEquals(ProjectUpdateKind.CURRENT, projectUpdate(project(), metadata(android="16")).kind)
    }
    @Test fun recoveryExportPreservesRetiredSelections() {
        val original = project().copy(selectedAppIds=setOf("retired"), selectedPackageAppSets=mapOf("retired" to "core"))
        assertEquals(ProjectUpdateKind.INCOMPATIBLE, projectUpdate(original, metadata()).kind)
        assertTrue(recoveryProjectConfig(original, metadata()).contains(">>retired=1"))
        assertEquals(setOf("retired"), original.selectedAppIds)
    }
}
