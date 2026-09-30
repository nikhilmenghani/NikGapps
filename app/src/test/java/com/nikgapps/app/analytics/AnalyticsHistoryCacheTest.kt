package com.nikgapps.app.analytics

import java.nio.file.Files
import java.time.Instant
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AnalyticsHistoryCacheTest {
    private val now = Instant.parse("2026-09-30T18:00:00Z").toEpochMilli()
    private fun row(id: String, timestamp: String = "2026-09-30T17:00:00.123456Z", count: Int = 2) =
        buildJsonArray {
            add(id); add(timestamp); add("build.zip"); add(count); add("model"); add("code")
            add(100L); add("Downloads/NikGapps"); add("rename"); add("user")
        }

    @Test fun persistsSnapshotsAndSeparatesProjectsAndRecoversCorruption() {
        val directory = Files.createTempDirectory("analytics-history").toFile()
        try {
            val cache = AnalyticsHistoryCache(directory, "https://us.posthog.com", "1")
            val history = AnalyticsHistory(listOf(row("a")), now, now)
            cache.write(history)
            assertEquals(history, cache.read())
            assertEquals(AnalyticsHistory(), AnalyticsHistoryCache(directory, "https://us.posthog.com", "2").read())
            assertEquals(AnalyticsHistory(), AnalyticsHistoryCache(directory, "https://eu.posthog.com", "1").read())
            cache.write(AnalyticsHistory(emptyList(), now, now))
            assertEquals(now, cache.read().syncedThrough)
            directory.listFiles()!!.single().writeText("broken JSON")
            assertEquals(AnalyticsHistory(), cache.read())
        } finally { directory.deleteRecursively() }
    }

    @Test fun mergesOverlapWithoutLosingHistoryOrDuplicatingCounts() {
        val previous = AnalyticsHistory(listOf(row("old", "2026-01-01T00:00:00Z"), row("recent")), now - 1000, now - 1000)
        var sql = ""
        val result = AnalyticsHistorySync().refresh(previous, now) {
            sql = it
            listOf(row("new", "2026-09-30T17:30:00Z"), row("recent", count = 3))
        }
        assertTrue(sql.contains("timestamp >= toDateTime64('2026-09-23T17:59:59Z', 6)"))
        assertTrue(sql.contains("timestamp < toDateTime64('2026-09-30T18:00:00Z', 6)"))
        assertEquals(listOf("new", "recent", "old"), result.rows.map { it[0].jsonPrimitive.content })
        assertEquals(3, result.rows[1][3].jsonPrimitive.int)
        assertEquals(now, result.syncedThrough)
    }

    @Test fun paginatesUsingUuidAndFullTimestampPrecision() {
        val queries = mutableListOf<String>()
        val first = row("00000000-0000-0000-0000-000000000002")
        val second = row("00000000-0000-0000-0000-000000000001")
        val result = AnalyticsHistorySync(2).refresh(AnalyticsHistory(), now) {
            queries += it
            if (queries.size == 1) listOf(first, second) else emptyList()
        }
        assertEquals(2, result.rows.size)
        assertFalse(queries[0].contains("timestamp >="))
        assertTrue(queries[1].contains("timestamp = toDateTime64('2026-09-30T17:00:00.123456Z', 6)"))
        assertTrue(queries[1].contains("uuid < toUUID('00000000-0000-0000-0000-000000000001')"))
    }

    @Test fun failedLaterPageLeavesCheckpointIntact() {
        val directory = Files.createTempDirectory("analytics-failed-sync").toFile()
        try {
            val cache = AnalyticsHistoryCache(directory, "host", "1")
            val previous = AnalyticsHistory(listOf(row("old")), now - 1000, now - 1000)
            cache.write(previous)
            var calls = 0
            try {
                val updated = AnalyticsHistorySync(1).refresh(previous, now) {
                    if (++calls == 1) listOf(row("new")) else error("network failed")
                }
                cache.write(updated)
                fail("Expected network failure")
            } catch (_: IllegalStateException) { }
            assertEquals(previous, cache.read())
        } finally { directory.deleteRecursively() }
    }

    @Test fun emptyRefreshAdvancesCheckpoint() {
        val previous = AnalyticsHistory(listOf(row("old")), now - 1000, now - 1000)
        val result = AnalyticsHistorySync().refresh(previous, now) { emptyList() }
        assertEquals(previous.rows, result.rows)
        assertEquals(now, result.syncedThrough)
    }

    @Test fun monthlyReconciliationFindsHistoricalUploads() {
        var sql = ""
        val previous = AnalyticsHistory(listOf(row("recent")), now - 1000, now - 31 * 86400000L)
        val result = AnalyticsHistorySync().refresh(previous, now) {
            sql = it
            listOf(row("late", "2026-01-01T00:00:00Z"))
        }
        assertFalse(sql.contains("timestamp >="))
        assertEquals(2, result.rows.size)
        assertEquals(now, result.reconciledAt)
    }
}
