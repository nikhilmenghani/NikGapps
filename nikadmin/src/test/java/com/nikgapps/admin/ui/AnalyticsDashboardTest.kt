package com.nikgapps.admin.ui

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AnalyticsDashboardTest {
    @Test fun userFilterUsesUsernameOrAnonymousId() {
        val events = analyticsDashboard(listOf(
            row("named", 30, JsonPrimitive("nikhil")),
            row("anonymous", 29, JsonNull),
            row("legacy", 28)
        )).events
        assertEquals(listOf("@nikhil", "user", "user"),
            events.map { it.filterValue(BuildFilterCategory.USER) })
        assertEquals(listOf("named"), events.filter {
            it.filterValue(BuildFilterCategory.USER) == "@nikhil"
        }.map { it.id })
        assertEquals(2, events.count { it.filterValue(BuildFilterCategory.USER) == "user" })
    }

    private fun row(id: String, day: Int, username: JsonElement? = null) = buildJsonArray {
        add(id); add("2026-09-${day}T12:00:00Z"); add("build.zip"); add(2)
        add("model"); add("code"); add(100); add("Downloads/NikGapps"); add("rename"); add("user")
        username?.let { add(it) }
    }

    @Test fun legacyAndNullUsernamesRemainAnonymous() {
        val dashboard = analyticsDashboard(listOf(row("old", 28), row("null", 29, JsonNull)))
        assertTrue(dashboard.events.all { it.githubUsername.isEmpty() })
        assertEquals("", dashboard.users.single().githubUsername)
        assertEquals(2, dashboard.users.single().zipCount)
    }

    @Test fun usesLatestKnownUsernameEvenAfterAnonymousBuild() {
        val dashboard = analyticsDashboard(listOf(
            row("newest", 30, JsonPrimitive("  ")),
            row("named", 29, JsonPrimitive(" new-name ")),
            row("older", 28, JsonPrimitive("old-name"))
        ))
        assertEquals("new-name", dashboard.users.single().githubUsername)
        assertEquals(3, dashboard.users.single().zipCount)
        assertEquals("user", dashboard.users.single().distinctId)
        assertEquals("new-name", dashboard.events[1].githubUsername)
        assertEquals("", dashboard.events[0].githubUsername)
    }
}
