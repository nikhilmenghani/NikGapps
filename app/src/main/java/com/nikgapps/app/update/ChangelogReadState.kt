package com.nikgapps.app.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nikgapps.app.utils.managers.prefMutableState
import java.security.MessageDigest

/** Track content, rather than versions: upcoming notes can change before a release. */
internal object ChangelogReadState {
    private var readRevisions by prefMutableState(
        keyName = "read_changelog_revisions",
        defaultValue = emptySet<String>(),
        getPreferencesKey = { stringSetPreferencesKey(it) }
    )

    fun hasUnreadUpcoming(entries: List<ChangelogEntry>, installed: String, published: String?): Boolean =
        hasUnreadUpcomingChanges(entries, installed, published, readRevisions)

    fun markRead(entries: List<ChangelogEntry>) {
        val updated = readRevisions + entries.map(::changelogRevision)
        if (updated != readRevisions) readRevisions = updated
    }
}

internal fun hasUnreadUpcomingChanges(
    entries: List<ChangelogEntry>,
    installed: String,
    published: String?,
    readRevisions: Set<String>
): Boolean = entries.any {
    changelogStatus(it.version, installed, published) == ChangelogStatus.UPCOMING &&
        changelogRevision(it) !in readRevisions
}

internal fun changelogRevision(entry: ChangelogEntry): String {
    // Dates and surrounding Markdown whitespace do not represent new release notes.
    val content = (listOf(entry.version.trim().removePrefix("v")) + entry.changes.map(String::trim))
        .joinToString("\u0000")
    return MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
