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
import com.nikgapps.app.data.BackgroundPrefs
import com.nikgapps.app.data.GlassBackground
import com.nikgapps.app.data.dataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
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

    @Test fun developerOptionsHideRemovedOverridesAndUseShortDescriptions() {
        check(instrumentation.targetContext.packageName == "com.nikgapps.review.debug")
        val originalDeveloper = DisplayPrefs.developerOptionsEnabled
        val originalGlass = DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(true)
        try {
            DisplayPrefs.developerOptionsEnabled = true
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val base = if (dark) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = if (DisplayPrefs.useLiquidGlass) base.withGlassAccent(dark) else base) {
                        SettingsScreen(onBack = {})
                    }
                } }
                click("Advanced")
                find { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                for (darkMode in listOf(true, false)) for (glass in listOf(true, false)) {
                    scenario.onActivity { dark = darkMode; DisplayPrefs.useLiquidGlass = glass }
                    awaitText("Try the app with regular-account limits")
                    awaitText("Require the latest app version")
                    awaitText("Require an internet connection to open the app")
                    assertNull(find { it.text?.toString()?.contains("Allow guest mode") == true })
                    assertNull(find { it.text?.toString()?.contains("Allow unsupported Android versions") == true })
                    capture("developer-options", darkMode, glass)
                }
            }
        } finally {
            DisplayPrefs.developerOptionsEnabled = originalDeveloper
            DisplayPrefs.useLiquidGlass = originalGlass
        }
    }

    @Test fun virtualMacIsVisibleInAccountAndHelpPreservesSettingsAcrossThemes() {
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val expected = requireNotNull(com.nikgapps.app.data.VirtualDeviceIdentity.read(context))
        val originalGlass = DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(true)
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val base = if (dark) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = if (DisplayPrefs.useLiquidGlass) base.withGlassAccent(dark) else base) {
                        SettingsScreen(onBack = {})
                    }
                } }
                click("Account")
                for (darkMode in listOf(true, false)) for (glass in listOf(true, false)) {
                    scenario.onActivity { dark = darkMode; DisplayPrefs.useLiquidGlass = glass }
                    awaitText(expected.virtualMac)
                    assertNull(find { it.text?.toString()?.contains("This virtual MAC address") == true })
                    var info = requireNotNull(find { it.contentDescription?.toString() == "About virtual MAC" })
                    while (!info.isClickable && info.parent != null) info = requireNotNull(info.parent)
                    check(info.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                    awaitText("This virtual MAC address is generated to uniquely identify your device across multiple app reinstalls.")
                    awaitText("Got it")
                    capture("virtual-mac-help", darkMode, glass)
                    click("Got it")
                    awaitText(expected.virtualMac)
                    capture("virtual-mac-account", darkMode, glass)
                    assertEquals(expected, com.nikgapps.app.data.VirtualDeviceIdentity.read(context))
                }
            }
        } finally { DisplayPrefs.useLiquidGlass = originalGlass }
    }

    @Test fun backgroundPickerPreviewsCancelsAndPersistsChoicesAcrossThemes() {
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val originalGlass = DisplayPrefs.useLiquidGlass
        val originalBackground = BackgroundPrefs.selected
        var dark by mutableStateOf(true)
        try {
            DisplayPrefs.useLiquidGlass = true
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val base = if (dark) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = base.withGlassAccent(dark, BackgroundPrefs.selected)) {
                        SettingsScreen(onBack = {})
                    }
                } }
                click("Appearance")
                click("Background")
                pickBackground("Dusk")
                assertEquals("Preview must not save a choice", originalBackground, BackgroundPrefs.selected)
                click("Cancel")
                awaitText("Liquid glass")
                assertEquals(originalBackground, BackgroundPrefs.selected)
                for (darkMode in listOf(true, false)) for (background in GlassBackground.entries) {
                    scenario.onActivity { dark = darkMode }
                    val before = BackgroundPrefs.selected
                    click("Background")
                    pickBackground(background.label)
                    assertEquals("Selecting a preview must not change the app", before, BackgroundPrefs.selected)
                    capture("background-${background.id}", darkMode, true)
                    click("Apply")
                    assertEquals(background, BackgroundPrefs.selected)
                    runBlocking {
                        withTimeout(3000) {
                            context.dataStore.data.first { it[stringPreferencesKey("glassBackground")] == background.id }
                        }
                    }
                    val scheme = (if (darkMode) darkColorScheme() else lightColorScheme()).withGlassAccent(darkMode, background)
                    assertTrue("Selected control colors must remain readable",
                        ColorUtils.calculateContrast(scheme.onPrimary.toArgb(), scheme.primary.toArgb()) >= 4.5)
                    assertTrue("Container labels must remain readable",
                        ColorUtils.calculateContrast(scheme.onPrimaryContainer.toArgb(), scheme.primaryContainer.toArgb()) >= 4.5)
                    // Applying a background must preserve the expanded Settings section.
                    awaitText("Liquid glass")
                }
            }
        } finally {
            BackgroundPrefs.selected = originalBackground
            DisplayPrefs.useLiquidGlass = originalGlass
        }
    }

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

    private fun pickBackground(label: String) {
        repeat(4) {
            if (find { it.isVisibleToUser && it.text?.toString() == label } != null) {
                click(label)
                return
            }
            find { it.isScrollable && it.isVisibleToUser }
                ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            instrumentation.waitForIdleSync()
        }
        click(label)
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
