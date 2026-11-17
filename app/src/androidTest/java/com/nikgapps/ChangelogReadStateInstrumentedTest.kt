package com.nikgapps

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.dataStore
import com.nikgapps.app.update.ChangelogDialog
import com.nikgapps.app.update.ChangelogEntry
import com.nikgapps.app.update.ChangelogReadState
import com.nikgapps.app.update.changelogRevision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChangelogReadStateInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun openingChangelogClearsBadgeAndPersistsReadContentButNewNotesHighlightAgain() {
        check(instrumentation.targetContext.packageName == "com.nikgapps.review.debug")
        val original = ChangelogEntry("99.1", listOf("Read-state regression ${UUID.randomUUID()}"))
        var entries by mutableStateOf(listOf(original))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    var showChangelog by remember { mutableStateOf(false) }
                    val unread = ChangelogReadState.hasUnreadUpcoming(entries, "1.0-debug", "1.0")
                    MaterialTheme {
                        Button(onClick = { showChangelog = true }, modifier = Modifier.semantics {
                            contentDescription = if (unread) "Changelog unread" else "Changelog read"
                        }) { Text("History") }
                        if (showChangelog) ChangelogDialog("Changelog", entries,
                            onDismiss = { showChangelog = false })
                    }
                }
            }
            click { it.contentDescription?.toString() == "Changelog unread" }
            await { !ChangelogReadState.hasUnreadUpcoming(entries, "1.0-debug", "1.0") }
            click { it.text?.toString() == "Close" }
            awaitNode { it.contentDescription?.toString() == "Changelog read" }
            val persisted = runBlocking {
                App.globalClass.dataStore.data.first()[stringSetPreferencesKey("read_changelog_revisions")]
            }
            assertTrue(persisted.orEmpty().contains(changelogRevision(original)))
            scenario.onActivity { entries = listOf(original.copy()) }
            awaitNode { it.contentDescription?.toString() == "Changelog read" }
            scenario.onActivity {
                entries = listOf(original.copy(changes = original.changes + "New notes in the same version"))
            }
            awaitNode { it.contentDescription?.toString() == "Changelog unread" }
        }
    }

    // Drive the actual dialog through Android accessibility; no Compose test coroutine scheduler is needed.
    private fun awaitNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        var found: AccessibilityNodeInfo? = null
        await {
            val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
            instrumentation.uiAutomation.rootInActiveWindow?.let { pending.add(it) }
            while (pending.isNotEmpty() && found == null) {
                val node = pending.removeFirst()
                if (matches(node)) found = node
                else repeat(node.childCount) { index -> node.getChild(index)?.let { pending.add(it) } }
            }
            found != null
        }
        return requireNotNull(found)
    }

    private fun click(matches: (AccessibilityNodeInfo) -> Boolean) {
        var node = awaitNode(matches)
        while (!node.isClickable && node.parent != null) node = requireNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun await(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Changelog read-state condition timed out" }
            Thread.sleep(50)
        }
    }
}
