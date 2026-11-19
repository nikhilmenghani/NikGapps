package com.nikgapps.app.registry

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class PackageReleaseNotesRepositoryTest {
    @get:Rule val cacheDirectory = TemporaryFolder()

    @Test fun returnsOnlyNotesForTheExactReleaseDate() = runBlocking {
        File(cacheDirectory.newFolder("nikgapps"), "package-release-notes.yaml").writeText("""
            - date: 11th Nov 2026 (Android 17)
              changes:
                - item: "Pixel Launcher: fixed search card colors"
                - item: >-
                    Restore calendar and contacts
                    sync adapters
            - date: 19th November 2026 (Android 17)
              changes:
                - item: Update WebView
        """.trimIndent())
        val directory = cacheDirectory.root
        assertEquals(listOf("Pixel Launcher: fixed search card colors", "Restore calendar and contacts sync adapters"),
            PackageReleaseNotesRepository.changesOn(directory, LocalDate.of(2026, 11, 11), online = false))
        assertTrue(PackageReleaseNotesRepository.changesOn(directory, LocalDate.of(2026, 11, 12), online = false).isEmpty())
        assertEquals(listOf("Update WebView"),
            PackageReleaseNotesRepository.changesOn(directory, LocalDate.of(2026, 11, 19), online = false))
    }

    @Test fun acceptsIsoDatesAndOmitsInvalidOrEmptyEntries() {
        val notes = PackageReleaseNotesRepository.parse("""
            - date: 2026-11-11
              changes:
                - item: New package
                - item: " "
            - date: 30th February 2026
              changes:
                - item: Invalid date
            - date: 12th Nov 2026
              changes: []
        """.trimIndent())
        assertEquals(mapOf(LocalDate.of(2026, 11, 11) to listOf("New package")), notes)
    }
}
