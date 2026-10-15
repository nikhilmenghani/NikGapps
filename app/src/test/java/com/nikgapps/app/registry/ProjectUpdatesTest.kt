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
    @Test fun platformEditCannotRestorePreviousReleaseOrConfiguration() {
        val previous = project().copy(androidVersion = AndroidVersion.ANDROID_16,
            lastBuiltReleaseId = "old-built", dismissedReleaseId = "old-dismissed", configSnapshot = "AndroidVersion=16")
        val edited = previous.copy(androidVersion = AndroidVersion.ANDROID_17).rebasePlatform(metadata())
        assertEquals(AndroidVersion.ANDROID_17, edited.androidVersion)
        assertEquals("new", edited.createdReleaseId)
        assertEquals("41", edited.configVersion)
        assertNull(edited.lastBuiltReleaseId); assertNull(edited.dismissedReleaseId); assertNull(edited.configSnapshot)
        assertEquals(previous.selectedAppIds, edited.selectedAppIds)
        assertEquals(previous.selectedPackageAppSets, edited.selectedPackageAppSets)
        assertEquals(ProjectUpdateKind.CURRENT, projectUpdate(edited, metadata()).kind)
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

    private fun summary(id: String = "new", date: String = "2026-10-11T12:00:00Z") =
        ReleaseSummary(id, "17", 37, "arm64-v8a", "stable", "test", date, "release.json")

    @Test fun matchingProjectImplicitlyAcknowledgesOnlyItsReleaseGroup() {
        val current = project().copy(createdReleaseId = "new")
        assertTrue(releaseKnownToProjects(summary(), listOf(current)))
        assertTrue(releaseKnownToProjects(summary(), listOf(project().copy(lastBuiltReleaseId = "new"))))
        assertFalse(releaseKnownToProjects(summary(), listOf(project())))
        assertFalse(releaseKnownToProjects(summary(), listOf(current.copy(androidVersion = AndroidVersion.ANDROID_16))))
        assertFalse(releaseKnownToProjects(summary(), listOf(current.copy(defaultChannel = "beta"))))
    }

    @Test fun bulkDismissalSkipsOtherVersionsFutureReleasesAndIncompatibleProjects() {
        val first = project().copy(id = "first")
        val older = project().copy(id = "older")
        val future = project().copy(id = "future")
        val incompatible = project().copy(id = "incompatible")
        val android16 = project().copy(id = "android16", androidVersion = AndroidVersion.ANDROID_16)
        val before = listOf(first, older, future, incompatible, android16)
        val olderMetadata = metadata(id = "previous").let {
            it.copy(release = it.release!!.copy(createdAt = "2026-10-10T12:00:00Z"))
        }
        val futureMetadata = metadata(id = "future").let {
            it.copy(release = it.release!!.copy(createdAt = "2026-10-12T12:00:00Z"))
        }
        val after = dismissProjectsThroughRelease(before, summary(), mapOf(
            first.id to metadata(), older.id to olderMetadata, future.id to futureMetadata,
            incompatible.id to metadata(digest = "changed"), android16.id to metadata(android = "16")))
        assertEquals(listOf("new", "new", null, null, null), after.map { it.dismissedReleaseId })
        assertEquals(before.map { it.createdReleaseId }, after.map { it.createdReleaseId })
    }

    @Test fun dismissalWatermarkCoversOlderReleaseButNotLaterRelease() {
        val dismissed = project().copy(dismissedReleaseId = "dismissed")
        val index = ReleaseIndex(1, "", emptyMap(), listOf(summary("dismissed", "2026-10-12T12:00:00Z")))
        assertEquals(ProjectUpdateKind.CURRENT, projectUpdate(dismissed, metadata().copy(releaseIndex = index)).kind)
        val newer = metadata(id = "next").let { it.copy(releaseIndex = index,
            release = it.release!!.copy(createdAt = "2026-10-13T12:00:00Z")) }
        assertEquals(ProjectUpdateKind.AVAILABLE, projectUpdate(dismissed, newer).kind)
        assertEquals(ProjectUpdateKind.INCOMPATIBLE, projectUpdate(dismissed, metadata(digest = "changed").copy(releaseIndex = index)).kind)
    }
}
