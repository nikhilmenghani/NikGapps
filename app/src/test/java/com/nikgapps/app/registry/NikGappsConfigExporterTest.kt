package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test
import com.nikgapps.app.data.*

class NikGappsConfigExporterTest {
    @Test fun keepAospUsesTwoOnlyForSelectedPackageEntries() {
        val template = "# Following are the packages\nCore=1\n>>GmsCore=1\n>>GooglePlayStore=1\nGoogleClock=1\nGoogleContacts=1\n"
        val output = NikGappsConfigExporter.render(template, "Android 17",
            mapOf("Core" to setOf("GmsCore", "GooglePlayStore"), "GoogleClock" to setOf("GoogleClock")),
            mapOf("Core" to setOf("GmsCore"), "GoogleClock" to setOf("GoogleClock"),
                "GoogleContacts" to setOf("GoogleContacts")))
        assertTrue(output.contains("Core=1\n>>GmsCore=2\n>>GooglePlayStore=1"))
        assertTrue(output.contains("GoogleClock=2"))
        assertTrue(output.contains("GoogleContacts=0"))
    }

    @Test fun exportNormalizesWindowsTemplatesToLf() {
        val template = "AndroidVersion=17\r\nVersion=40\r\n# Following are the packages\r\nCore=1\r\n>>GmsCore=1\r\n"
        val output = NikGappsConfigExporter.render(template, "Android 17", mapOf("Core" to setOf("GmsCore")))
        assertFalse(output.contains('\r'))
        assertTrue(output.endsWith("\n"))
        assertTrue(output.contains("Core=1\n>>GmsCore=1\n"))
    }

    @Test fun projectExportIncludesAutomaticallyResolvedDependencies() {
        val metadata = RegistryMetadata(
            CatalogParser.parseCatalog(RegistryTestFixtures.catalog("""{"id":"gms_core_extra_files"}""")),
            CatalogParser.parseAppSets(RegistryTestFixtures.appSets()), emptyMap(), null, null, false, 0L)
        val project = BuildProject(name = "Core", androidVersion = AndroidVersion.ANDROID_16,
            architecture = Architecture.ARM64, selectedAppIds = setOf("gms_core"),
            selectedPackageAppSets = mapOf("gms_core" to "core"))
        val template = "AndroidVersion=16\nVersion=40\n# Following are the packages\nCore=1\n>>GmsCore=1\n>>ExtraFiles=1\nCoreGo=1\n>>GmsCore=1\n"
        val output = NikGappsConfigExporter.forProject(template, project, metadata)
        assertTrue(output.contains("Core=1\n>>GmsCore=1\n>>ExtraFiles=1"))
        assertTrue(output.contains("CoreGo=0\n>>GmsCore=0"))
    }

    @Test fun exportPreservesSettingsAndDisablesUnselectedPackagesAndAlternateGroups() {
        val template = """# NikGapps configuration file
AndroidVersion=16
Version=40
Mode=install
WipeDalvikCache=1
UseZipConfig=0
# Following are the packages with default configuration
Core=1
>>GmsCore=1
>>GooglePlayStore=1
>>ExtraFiles=1
CoreGo=1
>>GmsCore=1
GoogleClock=1
SetupWizard=1
>>SetupWizard=1
PixelSetupWizard=1
>>SetupWizardPixel=1
"""
        val config = NikGappsConfigExporter.render(template, "Android 17",
            mapOf("Core" to setOf("GmsCore", "ExtraFiles"), "GoogleClock" to setOf("GoogleClock")))
        assertTrue(config.contains("AndroidVersion=17\nVersion=40\nMode=install\nWipeDalvikCache=1"))
        assertTrue(config.contains("Core=1\n>>GmsCore=1\n>>GooglePlayStore=0\n>>ExtraFiles=1"))
        assertTrue(config.contains("CoreGo=0\n>>GmsCore=0"))
        assertTrue(config.contains("GoogleClock=1"))
        assertTrue(config.contains("UseZipConfig=1"))
        assertFalse(config.contains("UseZipConfig=0"))
        assertTrue(config.contains("SetupWizard=0\n>>SetupWizard=0"))
        assertTrue(config.contains("PixelSetupWizard=0\n>>SetupWizardPixel=0"))
    }
}
