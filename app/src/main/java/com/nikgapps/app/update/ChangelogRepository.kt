package com.nikgapps.app.update

import com.nikgapps.app.utils.network.NetworkClient
import okhttp3.Request
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import java.io.File

data class ChangelogEntry(val version: String, val changes: List<String>, val date: String? = null)

object ChangelogRepository {
    private val cacheMutex = Mutex()
    suspend fun fetch(context: Context, forceRefresh: Boolean = false,
        reportErrors: Boolean = false): List<ChangelogEntry> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
        val cache = File(context.filesDir, "changelog.md")
        val cached = runCatching { parse(cache.readText()) }.getOrDefault(emptyList())
        if (!forceRefresh && cached.isNotEmpty()) return@withLock cached
        try {
        val request = Request.Builder().url(CHANGELOG_URL).build()
        NetworkClient.executeRequest(request).use { response ->
            if (!response.isSuccessful) error("Unable to load changelog (${response.code})")
            val text = response.body.string()
            val parsed = parse(text)
            require(parsed.isNotEmpty()) { "Downloaded changelog is empty" }
            val temporary = File(context.filesDir, "changelog.md.tmp")
            temporary.writeText(text)
            check(temporary.renameTo(cache)) { "Unable to save changelog cache" }
            parsed
        }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { if (reportErrors) throw error else cached }
        }
    }

    fun between(
        entries: List<ChangelogEntry>,
        installedVersion: String,
        targetVersion: String
    ): List<ChangelogEntry> = entries.filter {
        compareVersions(it.version, installedVersion) > 0 &&
            compareVersions(it.version, targetVersion) <= 0
    }.sortedWith { left, right -> compareVersions(right.version, left.version) }

    internal fun parse(text: String): List<ChangelogEntry> {
        val entries = mutableListOf<ChangelogEntry>()
        var version: String? = null
        var date: String? = null
        var changes = mutableListOf<String>()

        fun commit() {
            val currentVersion = version ?: return
            entries += ChangelogEntry(currentVersion, changes.toList(), date)
        }

        var inComment = false
        text.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("<!--")) inComment = true
            if (inComment) {
                if (trimmed.endsWith("-->") || trimmed.contains("-->")) inComment = false
                return@forEach
            }
            val heading = trimmed.trimStart('#').trim()
            val match = VERSION_PATTERN.matchEntire(heading)
            val matchedVersion = match?.groupValues?.get(1)
            when {
                matchedVersion != null -> {
                    commit()
                    version = matchedVersion
                    date = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                        ?.let { runCatching { java.time.LocalDate.parse(it).toString() }.getOrNull() }
                    changes = mutableListOf()
                }
                version != null && trimmed.isNotEmpty() && !trimmed.startsWith("#") ->
                    changes += trimmed.removePrefix("-").removePrefix("*").trim()
            }
        }
        commit()
        return entries
    }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = versionParts(left)
        val rightParts = versionParts(right)
        repeat(maxOf(leftParts.size, rightParts.size)) { index ->
            val comparison = leftParts.getOrElse(index) { 0 }
                .compareTo(rightParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun versionParts(version: String): List<Int> =
        version.trim().removePrefix("v").split('.', '-', '_').map { it.toIntOrNull() ?: 0 }

    // Optional release date: ## 0.80.18 — 2026-10-10.
    private val VERSION_PATTERN = Regex("^v?(\\d+(?:\\.\\d+)+)(?:\\s+[—|]\\s+(\\d{4}-\\d{2}-\\d{2}))?$", RegexOption.IGNORE_CASE)
    private const val CHANGELOG_URL =
        "https://raw.githubusercontent.com/nikhilmenghani/nikgapps/main/CHANGELOG.md"
}
