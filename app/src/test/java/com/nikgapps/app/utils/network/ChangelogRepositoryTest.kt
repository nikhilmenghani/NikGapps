package com.nikgapps.app.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangelogRepositoryTest {
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
