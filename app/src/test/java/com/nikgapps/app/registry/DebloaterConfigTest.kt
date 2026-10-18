package com.nikgapps.app.registry

import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.AndroidVersion
import com.nikgapps.app.data.Architecture
import org.junit.Assert.*
import org.junit.Test

class DebloaterConfigTest {
    @Test fun renderPreservesStandardBuilderCommentsBeforeSelectedTargets() {
        val template = requireNotNull(javaClass.getResource("/debloater-template.config"))
            .readText().replace("\r\n", "\n")
        val targets = listOf("YouTube", "/product/app/SomeApp")
        val rendered = DebloaterConfig.render(targets)
        assertEquals(template + "YouTube\n/product/app/SomeApp\n", rendered)
        assertEquals(targets, DebloaterConfig.parse(rendered))
        assertFalse(rendered.contains('\r'))
        assertEquals(template, DebloaterConfig.render(emptyList()))
        assertTrue(DebloaterConfig.parse(template).isEmpty())
    }

    @Test fun addUpdatesPreviewWithoutMutatingOrAcceptingInvalidEntries() {
        val empty = emptyList<String>()
        val first = DebloaterConfig.add(empty, " YouTube ")
        val next = DebloaterConfig.add(first, "/product/app/SomeApp/SomeApp.apk")
        assertTrue(empty.isEmpty())
        assertEquals(listOf("YouTube"), first)
        assertEquals(listOf("YouTube", "/product/app/SomeApp/SomeApp.apk"), next)
        assertEquals(next, DebloaterConfig.parse(DebloaterConfig.render(next)))
        for (input in listOf("YouTube", "", "YouTube\nMaps", "/system", "$(id)")) {
            assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.add(first, input) }
            assertEquals(listOf("YouTube"), first)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DebloaterConfig.add(empty, "/system/priv-app/SystemUI/SystemUI.apk")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DebloaterConfig.add(empty, "/product/app/SomeApp/SomeApp.apk", setOf("SomeApp"))
        }
    }
    @Test fun caseDistinctTargetsRemainDistinctAcrossAllConfigFormats() {
        val targets = DebloaterConfig.add(DebloaterConfig.add(emptyList(), "youtube"), "YouTube") +
            listOf("/product/app/youtube", "/product/app/YouTube")
        assertEquals(targets, DebloaterConfig.validate(targets))
        assertEquals(targets, DebloaterConfig.parse(DebloaterConfig.render(targets)))
        assertEquals(targets, DebloaterConfig.embeddedTargets(DebloaterConfig.withEmbeddedTargets("Version=41\n", targets)))
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.add(targets, "youtube") }
    }
    @Test fun configRoundTripUsesLfAndIgnoresComments() {
        val targets = listOf("YouTube", "/product/priv-app/SomeApp")
        assertEquals(targets, DebloaterConfig.parse("\uFEFF# Comment\r\nYouTube\r\n/product/priv-app/SomeApp\r\n"))
        assertEquals(targets, DebloaterConfig.parse(DebloaterConfig.render(targets)))
        assertFalse(DebloaterConfig.render(targets).contains('\r'))
        assertEquals(emptyList<String>(), DebloaterConfig.parse("# Nothing selected\n"))
    }

    @Test fun rejectsUnsafePathsAndShellSyntax() {
        listOf("/", "/system", "/system/app", "/data/app/Test", "/vendor/app/Test",
            "../Test", "/system/app/../Test", "Test/file.apk", "/system/app/Test//file.apk",
            "*", "Test?", "Test;id", "$(id)", "`id`", "Test name", "Test'", "Test\"", "Test\\path", "Test\u0000").forEach {
            assertThrows("Accepted $it", IllegalArgumentException::class.java) { DebloaterConfig.validate(listOf(it)) }
        }
    }

    @Test fun rejectsCriticalComponentsAndSelectionConflicts() {
        listOf("SystemUI", "Settings", "/system/priv-app/PermissionController", "FrameworkRes",
            "com.android.providers.settings", "PrebuiltGmsCore").forEach {
            assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.validate(listOf(it)) }
        }
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.validate(listOf("youtube"), setOf("YouTube")) }
        val clock = CatalogParser.parseCatalog(RegistryTestFixtures.catalog()).packages.first().copy(id = "google_clock")
        assertThrows(IllegalArgumentException::class.java) {
            DebloaterConfig.validate(listOf("DeskClock"), DebloaterConfig.conflicts(listOf(clock), setOf("google_clock")))
        }
    }

    @Test fun limitsAndDuplicatesAreEnforcedAtomically() {
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.parse("#".repeat(16385)) }
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.validate(List(65) { "App$it" }) }
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.parse("YouTube\nYouTube") }
        assertThrows(IllegalArgumentException::class.java) { DebloaterConfig.parse("YouTube\n/system\n") }
    }

    @Test fun editsPreserveProjectTargets() {
        val project = BuildProject(name = "Test", androidVersion = AndroidVersion.ANDROID_17,
            architecture = Architecture.ARM64, debloatTargets = listOf("YouTube"))
        assertEquals(project.debloatTargets, project.copy(name = "Renamed", selectedAppIds = setOf("clock")).debloatTargets)
    }
}
