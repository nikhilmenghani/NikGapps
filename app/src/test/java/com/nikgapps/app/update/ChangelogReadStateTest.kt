package com.nikgapps.app.update

import org.junit.Assert.*
import org.junit.Test

class ChangelogReadStateTest {
    private val upcoming = ChangelogEntry("0.81", listOf("Project backups"))
    private val installed = "0.80.22-debug"
    private val published = "0.80.22"

    private fun unread(entries: List<ChangelogEntry>, read: Set<String> = emptySet()) =
        hasUnreadUpcomingChanges(entries, installed, published, read)

    @Test fun `unread upcoming notes highlight until their content is read`() {
        assertTrue(unread(listOf(upcoming)))
        assertFalse(unread(listOf(upcoming), setOf(changelogRevision(upcoming))))
    }

    @Test fun `additional notes in the same version highlight again`() {
        val read = setOf(changelogRevision(upcoming))
        val changed = upcoming.copy(changes = upcoming.changes + "Improved sign-in")
        assertTrue(unread(listOf(changed), read))
        assertFalse(unread(listOf(changed), read + changelogRevision(changed)))
    }

    @Test fun `refreshing identical content and reordering releases stays read`() {
        val older = ChangelogEntry("0.80.21", listOf("Older notes"))
        val read = listOf(upcoming, older).map(::changelogRevision).toSet()
        assertFalse(unread(listOf(older, upcoming.copy()), read))
    }

    @Test fun `release dates and surrounding whitespace do not rehighlight read notes`() {
        val read = setOf(changelogRevision(upcoming))
        val reformatted = upcoming.copy(version = "v0.81", changes = listOf(" Project backups "), date = "2026-11-12")
        assertFalse(unread(listOf(reformatted), read))
    }

    @Test fun `a new release or newly loaded upcoming notes still highlight`() {
        val read = setOf(changelogRevision(upcoming))
        val next = upcoming.copy(version = "0.82")
        assertTrue(unread(listOf(upcoming, next), read))
        assertTrue(unread(listOf(next), read))
    }

    @Test fun `reading only one upcoming release does not hide another`() {
        val next = upcoming.copy(version = "0.82", changes = listOf("Another change"))
        assertTrue(unread(listOf(upcoming, next), setOf(changelogRevision(next))))
    }

    @Test fun `released installed and unknown publication states do not highlight`() {
        assertFalse(hasUnreadUpcomingChanges(listOf(upcoming), installed, "0.81", emptySet()))
        assertFalse(hasUnreadUpcomingChanges(listOf(upcoming), "0.81-debug", published, emptySet()))
        for (unknown in listOf(null, "Unknown")) {
            assertFalse(hasUnreadUpcomingChanges(listOf(upcoming), installed, unknown, emptySet()))
        }
        assertFalse(unread(emptyList()))
    }
}
