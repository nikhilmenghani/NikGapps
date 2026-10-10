package com.nikgapps.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogRepositoryTest {
    @Test fun `optional dates preserve older undated headings`() {
        val entries = ChangelogRepository.parse("## 0.80.18 — 2026-10-10\n- New timeline\n## 0.80.17\n- Previous change\n")
        assertEquals("2026-10-10", entries[0].date)
        assertEquals(listOf("New timeline"), entries[0].changes)
        assertEquals(null, entries[1].date)
        assertEquals("0.80.17", entries[1].version)
    }

    @Test fun `invalid optional date does not discard release history`() {
        val entries = ChangelogRepository.parse("## 0.80.18 — 2026-02-31\n- Change\n")
        assertEquals(null, entries.single().date)
        assertEquals(listOf("Change"), entries.single().changes)
    }

    private val changelog = """
        0.3
        feature B added
        ## 0.2
        - feature A added
        0.1
        initial release
    """.trimIndent()

    @Test
    fun `parses plain and markdown version headings`() {
        assertEquals(
            listOf(
                ChangelogEntry("0.3", listOf("feature B added")),
                ChangelogEntry("0.2", listOf("feature A added")),
                ChangelogEntry("0.1", listOf("initial release"))
            ),
            ChangelogRepository.parse(changelog)
        )
    }

    @Test
    fun `returns every version newer than the installed version through target`() {
        val entries = ChangelogRepository.parse(changelog)

        assertEquals(listOf("0.3", "0.2"), ChangelogRepository.between(entries, "0.1", "0.3").map { it.version })
        assertEquals(listOf("0.3"), ChangelogRepository.between(entries, "0.2", "0.3").map { it.version })
    }
}
