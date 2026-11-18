package com.nikgapps

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.presentation.ui.component.containers.GitHubAccountPreference
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog
import com.nikgapps.app.presentation.ui.screen.AccountGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Native accessibility avoids the Compose test clock's dependency on coroutine-test. */
class AccountDialogInstrumentedTest {
    @Test fun accountPopupsUseThemedActionsAndPreserveTheirUnderlyingLayout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val originalAuth = listOf(GithubPrefs.token, GithubPrefs.username, GithubPrefs.lastUsername,
            GithubPrefs.avatarUrl, GithubPrefs.guestMode.toString())
        val density = context.resources.displayMetrics.density
        var dark by mutableStateOf(true)
        var screen by mutableStateOf("gate")
        var exampleOpen by mutableStateOf(false)
        var externalActions = 0
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Surface(Modifier.fillMaxSize()) {
                        when (screen) {
                            "gate" -> AccountGate(onOpenSettings = { externalActions++ }, onContinueAsGuest = { externalActions++ })
                            "account" -> Column(Modifier.padding(24.dp)) { GitHubAccountPreference() }
                            else -> Column(Modifier.padding(24.dp)) {
                                Button(onClick = { exampleOpen = true }) { Text("Open dialog fixture") }
                                if (exampleOpen) AccountDialog(
                                    title = "GitHub account",
                                    onDismiss = { exampleOpen = false },
                                    content = { Text("Dialog layout fixture; no account request is made.") },
                                    actions = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilledTonalButton(onClick = { exampleOpen = false },
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(16.dp)) { Text("Close") }
                                            Button(onClick = { exampleOpen = false },
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(16.dp)) { Text("Open GitHub") }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } }
            for (darkTheme in listOf(true, false)) {
                scenario.onActivity { dark = darkTheme; screen = "gate" }
                val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
                val suffix = if (darkTheme) "dark" else "light"
                val underlying = bounds(awaitStableText("Why GitHub?"))
                clickText("Why GitHub?")
                val gotIt = clickable(awaitStableText("Got it"))
                val helpImage = capture("github-help-$suffix.png")
                assertAction(helpImage, bounds(gotIt), density, scheme.primary.toArgb())
                assertDivider(helpImage, bounds(gotIt), density, scheme.outlineVariant.toArgb())
                helpImage.recycle()
                clickText("Got it")
                awaitDialogDismissed("Got it", "Why GitHub?", underlying)

                scenario.onActivity { screen = "fixture" }
                val fixtureAnchor = bounds(awaitStableText("Open dialog fixture"))
                clickText("Open dialog fixture")
                val secondary = bounds(clickable(awaitStableText("Close")))
                val primary = bounds(clickable(awaitStableText("Open GitHub")))
                assertTrue("Paired actions must have equal widths", abs(primary.width() - secondary.width()) <= 1)
                assertEquals("Paired actions should share a baseline", secondary.top, primary.top)
                val pairedImage = capture("github-paired-actions-$suffix.png")
                assertAction(pairedImage, primary, density, scheme.primary.toArgb())
                assertAction(pairedImage, secondary, density, scheme.secondaryContainer.toArgb())
                assertDivider(pairedImage, Rect(secondary.left, secondary.top, primary.right, primary.bottom),
                    density, scheme.outlineVariant.toArgb())
                pairedImage.recycle()
                clickText("Close")
                awaitDialogDismissed("Close", "Open dialog fixture", fixtureAnchor)

                // Opening the settings preference only displays account state. Never tap Sign in / Switch account.
                scenario.onActivity { screen = "account" }
                val accountAnchor = bounds(awaitStableText("GitHub account"))
                clickText("GitHub account")
                val close = bounds(clickable(awaitStableText("Close")))
                val accountImage = capture("github-account-$suffix.png")
                assertAction(accountImage, close, density,
                    if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) scheme.primary.toArgb() else scheme.secondaryContainer.toArgb())
                assertDivider(accountImage, close, density, scheme.outlineVariant.toArgb())
                accountImage.recycle()
                clickText("Close")
                awaitDialogDismissed("Close", "GitHub account", accountAnchor)
            }
        }
        assertEquals(0, externalActions)
        assertEquals("Rendering dialogs must not change account credentials", originalAuth,
            listOf(GithubPrefs.token, GithubPrefs.username, GithubPrefs.lastUsername,
                GithubPrefs.avatarUrl, GithubPrefs.guestMode.toString()))
    }

    private fun assertAction(image: Bitmap, bounds: Rect, density: Float, color: Int) {
        assertTrue("Action must have at least 48dp height", bounds.height() >= (48 * density).toInt())
        assertTrue("Action must use its theme's filled color",
            matchesColor(image.getPixel(bounds.left + (8 * density).toInt(), bounds.centerY()), color))
    }

    private fun assertDivider(image: Bitmap, actionBounds: Rect, density: Float, color: Int) {
        val top = (actionBounds.top - 24 * density).toInt().coerceAtLeast(0)
        val hasDivider = (top until actionBounds.top).any { y ->
            val matchingPixels = (actionBounds.left until actionBounds.right).count { x ->
                matchesColor(image.getPixel(x, y), color)
            }
            matchingPixels > actionBounds.width() * 0.9f
        }
        assertTrue("A themed horizontal divider must span the action area", hasDivider)
    }

    private fun matchesColor(actual: Int, expected: Int) =
        abs(AndroidColor.red(actual) - AndroidColor.red(expected)) <= 4 &&
            abs(AndroidColor.green(actual) - AndroidColor.green(expected)) <= 4 &&
            abs(AndroidColor.blue(actual) - AndroidColor.blue(expected)) <= 4

    private fun capture(name: String): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(300)
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return image
    }

    private fun findText(text: String): AccessibilityNodeInfo? {
        val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(pending::add)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.text?.toString() == text) return node
            repeat(node.childCount) { node.getChild(it)?.let(pending::add) }
        }
        return null
    }

    private fun awaitText(text: String): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (true) {
            findText(text)?.let { return it }
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Control not found: $text" }
            Thread.sleep(25)
        }
    }

    private fun awaitStableText(text: String, absentText: String? = null): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        var previousBounds: Rect? = null
        var stableSince = 0L
        while (true) {
            val now = android.os.SystemClock.uptimeMillis()
            val node = findText(text)
            if (node != null && node.isVisibleToUser && (absentText == null || findText(absentText) == null)) {
                val currentBounds = bounds(node)
                if (!currentBounds.isEmpty && currentBounds == previousBounds) {
                    if (now - stableSince >= 200) return node
                } else {
                    previousBounds = currentBounds
                    stableSince = now
                }
            } else {
                previousBounds = null
                stableSince = now
            }
            check(now < deadline) { "Control did not settle: $text (must be absent: $absentText)" }
            Thread.sleep(25)
        }
    }

    private fun awaitDialogDismissed(action: String, anchor: String, originalBounds: Rect) {
        // The dialog window can remain active briefly after Compose removes its content.
        // Wait for its action to disappear before matching an identically named title/anchor.
        val underlying = awaitStableText(anchor, absentText = action)
        assertEquals("Popup dismissal moved the underlying layout", originalBounds, bounds(underlying))
    }

    private fun clickable(start: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var node = start
        while (!node.isClickable && node.parent != null) node = requireNotNull(node.parent)
        return node
    }

    private fun bounds(node: AccessibilityNodeInfo) = Rect().also(node::getBoundsInScreen)

    private fun clickText(text: String) {
        check(clickable(awaitText(text)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
