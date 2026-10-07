package com.nikgapps.admin.analytics

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.json.*

/** A project-scoped history snapshot. Read credentials are never persisted. */
internal data class AnalyticsHistory(
    val rows: List<JsonArray> = emptyList(),
    val syncedThrough: Long? = null,
    val reconciledAt: Long? = null
)

internal class AnalyticsHistoryCache(directory: File, host: String, projectId: String) {
    private val identity = "$host/$projectId"
    private val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private val file = File(directory, "analytics-$hash.json")

    fun read(): AnalyticsHistory = runCatching {
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        require(root["version"]?.jsonPrimitive?.int == 1 && root["project"]?.jsonPrimitive?.content == identity)
        val rows = root.getValue("rows").jsonArray.map { it.jsonArray }
        rows.forEach {
            require(it.size >= 10 && it[0].jsonPrimitive.content.isNotBlank())
            Instant.parse(it[1].jsonPrimitive.content)
        }
        AnalyticsHistory(rows, root["syncedThrough"]?.jsonPrimitive?.longOrNull,
            root["reconciledAt"]?.jsonPrimitive?.longOrNull)
    }.getOrElse { AnalyticsHistory() }

    fun write(history: AnalyticsHistory) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temporary = File(file.parentFile, "${file.name}.tmp")
        val json = buildJsonObject {
            put("version", 1); put("project", identity)
            history.syncedThrough?.let { put("syncedThrough", it) }
            history.reconciledAt?.let { put("reconciledAt", it) }
            put("rows", JsonArray(history.rows))
        }.toString()
        temporary.outputStream().use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING)
    }
}

/** Fetches only the unsynced interval, with a seven-day overlap for delayed ingestion. */
internal class AnalyticsHistorySync(private val pageSize: Int = 500) {
    fun refresh(previous: AnalyticsHistory, now: Long, query: (String) -> List<JsonArray>): AnalyticsHistory {
        // Periodically check all history for offline uploads older than the overlap.
        val reconcile = previous.reconciledAt == null || previous.rows.any { it.size < 11 } ||
            now < (previous.syncedThrough ?: 0) ||
            now - previous.reconciledAt >= RECONCILE_INTERVAL
        val since = if (reconcile) null else previous.syncedThrough?.minus(OVERLAP)
        val incoming = mutableListOf<JsonArray>()
        var cursor: JsonArray? = null
        do {
            val page = query(buildQuery(since, now, cursor))
            page.forEach { require(it.size >= 10 && it[0].jsonPrimitive.content.isNotBlank()) {
                "PostHog returned an invalid analytics event"
            } }
            incoming += page
            val next = page.lastOrNull()
            check(page.isEmpty() || next != cursor) { "PostHog pagination did not advance" }
            cursor = next
        } while (page.size == pageSize)
        // Advance only after every page succeeds; failed refreshes leave the checkpoint intact.
        val merged = (previous.rows + incoming).associateBy { it[0].jsonPrimitive.content }.values
            .sortedWith(compareByDescending<JsonArray> { Instant.parse(it[1].jsonPrimitive.content) }
                .thenByDescending { it[0].jsonPrimitive.content })
        return AnalyticsHistory(merged, now, if (reconcile) now else previous.reconciledAt)
    }

    private fun buildQuery(since: Long?, until: Long, cursor: JsonArray?): String {
        val lower = since?.let { "AND timestamp >= toDateTime64('${Instant.ofEpochMilli(it)}', 6)" }.orEmpty()
        val position = cursor?.let {
            val timestamp = it[1].jsonPrimitive.content.sqlLiteral()
            val uuid = it[0].jsonPrimitive.content.sqlLiteral()
            """AND (timestamp < toDateTime64('$timestamp', 6)
                OR (timestamp = toDateTime64('$timestamp', 6) AND uuid < toUUID('$uuid')))"""
        }.orEmpty()
        return """
            SELECT uuid, timestamp, properties.zip_name, properties.package_count,
                   coalesce(properties.device_model, properties.${'$'}device_model, '') AS device_model,
                   coalesce(properties.device_code, properties.${'$'}device_name, '') AS device_code,
                   properties.size_bytes, coalesce(properties.location, 'Downloads/NikGapps'),
                   properties.conflict_resolution, distinct_id,
                   coalesce(properties.github_username, '') AS github_username
            FROM events
            WHERE event = 'zip_creation_succeeded'
            AND timestamp < toDateTime64('${Instant.ofEpochMilli(until)}', 6)
            $lower
            $position
            ORDER BY timestamp DESC, uuid DESC
            LIMIT $pageSize
        """.trimIndent()
    }

    private fun String.sqlLiteral() = replace("\\", "\\\\").replace("'", "''")

    companion object {
        private const val OVERLAP = 7 * 24 * 60 * 60 * 1000L
        private const val RECONCILE_INTERVAL = 30 * 24 * 60 * 60 * 1000L
    }
}
