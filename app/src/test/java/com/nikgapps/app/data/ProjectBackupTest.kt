package com.nikgapps.app.data

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ProjectBackupTest {
    private val source = BackupSource("0.80.22-debug", 80022, "debug")
    private fun project(name: String = "Pixel build") = BuildProject(
        name = name, androidVersion = AndroidVersion.ANDROID_17, architecture = Architecture.ARM64,
        selectedAppSetId = "core", selectedAppIds = setOf("gms_core", "google_chrome"),
        selectedPackageAppSets = mapOf("gms_core" to "core", "google_chrome" to "google_chrome"),
        channelOverrides = mapOf("gms_core" to "beta"), keepAospCounterparts = setOf("google_chrome"),
        configVersion = "41", createdReleaseId = "17-arm64-stable-20261012", lastBuiltReleaseId = "17-arm64-stable-20261012",
        dismissedReleaseId = "17-arm64-stable-20261012", configSnapshot = "Version=41\nGmsCore=1\n",
        packageDefinitions = mapOf("gms_core" to "definition-sha"),
        installerSettings = mapOf("InstallPartition" to "product"), debloatTargets = listOf("YouTube"),
        appSources = mapOf("gms_core" to AppSourceConfig(AppSource.DEVICE)), createdAt = 123456L)
    private fun modify(text: String, change: (MutableMap<String, JsonElement>) -> Unit): String {
        val values = Json.parseToJsonElement(text).jsonObject.toMutableMap()
        change(values)
        return JsonObject(values).toString()
    }

    @Test fun roundTripPreservesProjectFieldsAndSourceRelease() {
        val original = project()
        val data = ProjectBackups.read(ProjectBackups.export(listOf(original), source, "2026-11-02T20:00:00Z"))
        assertEquals(listOf(original), data.projects)
        assertEquals(source, data.source)
        assertEquals(1, data.formatVersion)
        assertEquals("2026-11-02T20:00:00Z", data.exportedAt)
    }
    @Test fun releaseAndDebugVersionsDoNotGateCompatibleFormats() {
        for (version in listOf("0.80.22-debug", "0.81", "1.0-debug")) {
            val backup = ProjectBackups.read(ProjectBackups.export(listOf(project()), source.copy(appVersion = version)))
            assertEquals(1, backup.projects.size)
        }
    }
    @Test fun futureFormatOrReaderIsRejectedBeforeRestoring() {
        val text = ProjectBackups.export(listOf(project()), source)
        for (field in listOf("formatVersion", "minimumReaderVersion")) {
            val future = modify(text) { it[field] = JsonPrimitive(2) }
            val error = assertThrows(IllegalArgumentException::class.java) { ProjectBackups.read(future) }
            assertTrue(error.message.orEmpty().contains("Update NikGapps"))
        }
    }
    @Test fun rejectsWrongTypeCorruptionOversizedAndMalformedBackups() {
        val text = ProjectBackups.export(listOf(project()), source)
        for (bad in listOf("not JSON", " ".repeat(ProjectBackups.MAX_BYTES + 1),
            modify(text) { it["type"] = JsonPrimitive("other-app") },
            modify(text) { it["projects"] = JsonArray(emptyList()) })) {
            assertThrows(Exception::class.java) { ProjectBackups.read(bad) }
        }
    }
    @Test fun sourcePathsAreNotTransferredAndCredentialsAreNotPartOfEnvelope() {
        val original = project().let { it.copy(appSources = mapOf("gms_core" to AppSourceConfig(AppSource.DEVICE,
            "/data/user/0/com.nikgapps.debug/private.apk"))) }
        val text = ProjectBackups.export(listOf(original), source)
        assertFalse(text.contains("/data/user/"))
        assertFalse(text.contains("token", ignoreCase = true))
        assertEquals("", ProjectBackups.read(text).projects.single().appSources.getValue("gms_core").location)
    }
    @Test fun replacementContainsOnlyBackupProjectsWithoutSelectionOrMerge() {
        val incoming = listOf(project("First"), project("Second"))
        assertEquals(incoming, ProjectBackups.replacement(incoming, false))
    }
    @Test fun selectiveReplacementRequiresEliteAndCannotIncludeOutdatedIds() {
        val incoming = listOf(project("First"), project("Second"))
        val one = setOf(incoming.first().id)
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.replacement(incoming, false, one) }
        assertEquals(listOf(incoming.first()), ProjectBackups.replacement(incoming, true, one))
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.replacement(incoming, true, setOf("outdated")) }
    }
    @Test fun replacementHonorsLimitAndRejectsEmptyWithoutPartialResults() {
        val incoming = (1..6).map { project("Project $it") }
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.replacement(incoming, false) }
        assertEquals(incoming, ProjectBackups.replacement(incoming, true))
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.replacement(emptyList(), true) }
    }
    @Test fun damagedProjectDoesNotProducePartialImportAndEmptyBackupIsValid() {
        val original = project()
        val text = ProjectBackups.export(listOf(original, project("Other")), source)
        // Exporting an invalid enum is impossible; changing signed content is detected before decoding.
        assertThrows(IllegalArgumentException::class.java) { ProjectBackups.read(text.replace("ANDROID_17", "ANDROID_99")) }
        assertTrue(ProjectBackups.read(ProjectBackups.export(emptyList(), source)).projects.isEmpty())
    }
}
