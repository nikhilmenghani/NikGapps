package com.nikgapps

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.presentation.ui.component.GlassScaffold
import com.nikgapps.app.presentation.ui.component.GlassButton
import com.nikgapps.app.presentation.ui.component.GlassTonalButton
import com.nikgapps.app.presentation.ui.component.ProjectsBackdrop
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog
import com.nikgapps.app.presentation.theme.withGlassAccent
import com.nikgapps.app.presentation.ui.component.cards.PermissionsCard
import com.nikgapps.app.presentation.ui.screen.*
import com.nikgapps.app.registry.CatalogPackage
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GlassPagesInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun glassActionsPreserveEnabledStateAndDialogLayoutAcrossThemes() {
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val original = DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(true)
        var open by mutableStateOf(false)
        var confirmed = 0
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val scheme = if (dark) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = if (DisplayPrefs.useLiquidGlass) scheme.withGlassAccent(dark) else scheme) {
                        ProjectsBackdrop {
                            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                                GlassTonalButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("Open popup") }
                                GlassButton(onClick = { error("Disabled action invoked") }, enabled = false) { Text("Unavailable") }
                            }
                            if (open) AccountDialog(title = "Glass actions", onDismiss = { open = false },
                                content = { Text("Confirm this preview. No account or project data is changed.") },
                                actions = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GlassTonalButton(onClick = { open = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                                        GlassButton(onClick = { confirmed++; open = false }, modifier = Modifier.weight(1f)) { Text("Confirm") }
                                    }
                                })
                        }
                    }
                } }
                for (darkMode in listOf(true, false)) for (glass in listOf(true, false)) {
                    scenario.onActivity { dark = darkMode; DisplayPrefs.useLiquidGlass = glass }
                    val anchor = android.graphics.Rect().also { awaitText("Open popup").getBoundsInScreen(it) }
                    val unavailable = awaitText("Unavailable")
                    assertTrue("Unavailable action must stay disabled",
                        generateSequence(unavailable) { it.parent }.any { !it.isEnabled })
                    click("Open popup")
                    val first = android.graphics.Rect().also { actionNode("Cancel").getBoundsInScreen(it) }
                    val second = android.graphics.Rect().also { actionNode("Confirm").getBoundsInScreen(it) }
                    assertEquals(first.top, second.top)
                    assertTrue(kotlin.math.abs(first.width() - second.width()) <= context.resources.displayMetrics.density)
                    assertTrue(second.height() >= 48 * context.resources.displayMetrics.density)
                    capture("actions", darkMode, glass)
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = !glass }
                    click("Confirm")
                    val returned = android.graphics.Rect().also { awaitText("Open popup").getBoundsInScreen(it) }
                    assertEquals("Popup dismissal must preserve its underlying layout", anchor, returned)
                }
                assertEquals(4, confirmed)
            }
        } finally { DisplayPrefs.useLiquidGlass = original }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun settingsPackagesAndPermissionsWorkAcrossAppearanceChanges() {
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val original = DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(true)
        var page by mutableStateOf("settings")
        var expanded by mutableStateOf(false)
        var selected by mutableStateOf(false)
        var granted by mutableStateOf(false)
        var selectedEvents = 0
        val pkg = CatalogPackage("clock", "Google Clock", true, false, emptyList(), emptyMap(), emptyMap())
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                        when (page) {
                            "settings" -> SettingsScreen(onBack = {})
                            "packages" -> GlassScaffold(topBar = { TopAppBar(title = { Text("Package selection preview") }) }) { padding ->
                                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppSetHeader("Core", 1, if (selected) 1 else 0, expanded, true,
                                        onToggleExpanded = { expanded = !expanded }, onToggleSelection = { selected = !selected })
                                    AppSetContents(expanded) {
                                        ProjectPackageCard(pkg, RegistryDeviceStatus(false), selected,
                                            onOpen = {}, onSelected = { selected = it; selectedEvents++ },
                                            keepAosp = false, onKeepAospChange = {}, embedded = true)
                                    }
                                }
                            }
                            else -> GlassScaffold { padding ->
                                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                    PermissionsCard(isPermissionGranted = granted,
                                        onRequestPermission = { granted = true })
                                }
                            }
                        }
                    }
                } }
                for (darkMode in listOf(true, false)) for (glass in listOf(true, false)) {
                    scenario.onActivity { dark = darkMode; DisplayPrefs.useLiquidGlass = glass; page = "settings" }
                    awaitText("Appearance")
                    assertNull(find { it.text?.toString() == "Liquid glass" })
                    click("Appearance")
                    awaitText("Liquid glass")
                    // Changing treatment must preserve the open accordion and its clickable children.
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = !glass }
                    awaitText("Liquid glass")
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = glass }
                    capture("settings", darkMode, glass)
                    scenario.onActivity { page = "packages"; expanded = false; selected = false }
                    click("Core")
                    click("Google Clock")
                    assertTrue(selected)
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = !glass }
                    awaitText("Google Clock")
                    assertTrue(expanded)
                    click("Google Clock")
                    assertFalse(selected)
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = glass }
                    capture("packages", darkMode, glass)
                    scenario.onActivity { page = "permissions"; granted = false }
                    click("Allow access")
                    awaitText("Allowed")
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = !glass }
                    awaitText("Allowed")
                    assertTrue(granted)
                    scenario.onActivity { DisplayPrefs.useLiquidGlass = glass }
                    capture("permissions", darkMode, glass)
                }
                assertEquals(8, selectedEvents)
            }
        } finally { DisplayPrefs.useLiquidGlass = original }
    }

    private fun find(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
        instrumentation.uiAutomation.rootInActiveWindow?.let(pending::add)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (matches(node)) return node
            repeat(node.childCount) { node.getChild(it)?.let(pending::add) }
        }
        return null
    }

    private fun awaitText(text: String): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (true) {
            find { it.text?.toString()?.lineSequence()?.any { line -> line == text } == true }?.let { return it }
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Missing glass-page control: $text" }
            Thread.sleep(25)
        }
    }

    private fun actionNode(text: String): AccessibilityNodeInfo {
        var node = awaitText(text)
        while (!node.isClickable && node.parent != null) node = requireNotNull(node.parent)
        return node
    }

    private fun click(text: String) {
        val node = actionNode(text)
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun capture(page: String, dark: Boolean, glass: Boolean) {
        instrumentation.waitForIdleSync()
        Thread.sleep(400)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val filename = "${if (glass) "glass" else "classic"}-$page-${if (dark) "dark" else "light"}.png"
        File(instrumentation.targetContext.getExternalFilesDir(null), filename).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
