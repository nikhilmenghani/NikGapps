package com.nikgapps.app.update

import com.nikgapps.app.utils.network.NetworkClient
import okhttp3.Request

data class ChangelogEntry(val version: String, val changes: List<String>)

object ChangelogRepository {
    suspend fun fetch(): List<ChangelogEntry> = runCatching {
        val request = Request.Builder().url(CHANGELOG_URL).build()
        NetworkClient.executeRequest(request).use { response ->
            if (!response.isSuccessful) error("Unable to load changelog (${response.code})")
            parse(response.body.string())
        }
    }.getOrDefault(emptyList())

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
        var changes = mutableListOf<String>()

        fun commit() {
            val currentVersion = version ?: return
            entries += ChangelogEntry(currentVersion, changes.toList())
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
            val matchedVersion = VERSION_PATTERN.matchEntire(heading)?.groupValues?.get(1)
            when {
                matchedVersion != null -> {
                    commit()
                    version = matchedVersion
                    changes = mutableListOf()
                }
                version != null && trimmed.isNotEmpty() && !trimmed.startsWith("#") ->
                    changes += trimmed.removePrefix("-").removePrefix("*").trim()
            }
        }
        commit()
        return entries
    }

    private fun compareVersions(left: String, right: String): Int {
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

    private val VERSION_PATTERN = Regex("^v?(\\d+(?:\\.\\d+)+)$", RegexOption.IGNORE_CASE)
    private const val CHANGELOG_URL =
        "https://raw.githubusercontent.com/nikhilmenghani/nikgapps/main/CHANGELOG.md"
}
