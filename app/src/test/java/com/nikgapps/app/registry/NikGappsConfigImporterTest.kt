package com.nikgapps.app.registry

import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test

class NikGappsConfigImporterTest {
    private val metadata = RegistryMetadata(
        CatalogParser.parseCatalog(RegistryTestFixtures.catalog("""{"id":"gms_core_extra_files"}""")),
        CatalogParser.parseAppSets(RegistryTestFixtures.appSets()), emptyMap(), null, null, false, 0L)
    private val project = BuildProject(name = "Import", androidVersion = AndroidVersion.ANDROID_16,
        architecture = Architecture.ARM64)
    private val template = "AndroidVersion=16\nVersion=40\n# Following are the packages\nCore=1\n>>GmsCore=1\n>>ExtraFiles=1\nCoreGo=0\n>>GmsCore=0\n>>ExtraFilesGo=0\n"

    @Test fun importsSelectionsAndKeepAospWithoutSelectingInternalDependencies() {
        val imported = NikGappsConfigImporter.forProject(template.replace(">>GmsCore=1", ">>GmsCore=2"), template, project, metadata)
        assertEquals(setOf("gms_core"), imported.selectedAppIds)
        assertEquals(mapOf("gms_core" to "core"), imported.selectedPackageAppSets)
        assertEquals(setOf("gms_core"), imported.keepAospCounterparts)
        assertEquals(project.name, imported.name)
    }

    @Test fun disabledGroupDoesNotSelectChildren() {
        val imported = NikGappsConfigImporter.forProject(template.replace("Core=1\n", "Core=0\n"), template, project, metadata)
        assertTrue(imported.selectedAppIds.isEmpty())
        assertTrue(imported.keepAospCounterparts.isEmpty())
    }

    @Test fun exportedConfigRoundTripsSelections() {
        val selected = project.copy(selectedAppIds = setOf("gms_core"),
            selectedPackageAppSets = mapOf("gms_core" to "core"), keepAospCounterparts = setOf("gms_core"))
        val exported = NikGappsConfigExporter.forProject(template, selected, metadata)
        val imported = NikGappsConfigImporter.forProject(exported, template, project, metadata)
        assertEquals(selected.selectedAppIds, imported.selectedAppIds)
        assertEquals(selected.keepAospCounterparts, imported.keepAospCounterparts)
    }

    @Test fun rejectsOlderAndNewerVersionsAndOtherAndroidVersions() {
        listOf(template.replace("Version=40", "Version=39"), template.replace("Version=40", "Version=41"),
            template.replace("AndroidVersion=16", "AndroidVersion=17")).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                NikGappsConfigImporter.forProject(invalid, template, project, metadata)
            }
        }
    }

    @Test fun rejectsConflictingAppSetsUnknownPackagesAndInvalidFlags() {
        listOf(template.replace("CoreGo=0", "CoreGo=1").replace(">>GmsCore=0", ">>GmsCore=1"),
            template + "Unknown=1\n", template.replace(">>GmsCore=1", ">>GmsCore=3"),
            template + ">>ExtraFilesGo=0\n").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                NikGappsConfigImporter.forProject(invalid, template, project, metadata)
            }
        }
    }
}
