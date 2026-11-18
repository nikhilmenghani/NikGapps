package com.nikgapps

import android.graphics.Bitmap
import android.view.KeyEvent
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
import com.nikgapps.app.presentation.ui.component.StockAppRemovalDialog
import com.nikgapps.app.presentation.ui.screen.ProjectPackageCard
import com.nikgapps.app.presentation.ui.screen.RegistryDeviceStatus
import com.nikgapps.app.registry.CatalogPackage
import com.nikgapps.app.registry.withStockAppPreference
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.data.AndroidVersion
import com.nikgapps.app.data.Architecture
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StockAppOptionsInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun packageAndNavigationMenusHaveShapedShadowsAcrossAppearances() {
        check(instrumentation.targetContext.packageName == "com.nikgapps.review.debug")
        val originalGlass = com.nikgapps.app.data.DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(false)
        var more by mutableStateOf(false)
        var projectMenu by mutableStateOf(false)
        var invoked = 0
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                        com.nikgapps.app.presentation.ui.component.ProjectsBackdrop {
                            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box {
                                    com.nikgapps.app.presentation.ui.component.GlassTonalButton({ more = true }) { Text("More options") }
                                    com.nikgapps.app.presentation.ui.component.GlassDropdownMenu(more, { more = false }) {
                                        DropdownMenuItem(text = { Text("Logs") }, onClick = { invoked++ })
                                    }
                                }
                                Box {
                                    com.nikgapps.app.presentation.ui.component.GlassTonalButton({ projectMenu = true }) { Text("Project actions") }
                                    if (projectMenu) com.nikgapps.app.presentation.ui.screen.ProjectContextMenu("Example",
                                        listOf(com.nikgapps.app.presentation.ui.screen.ProjectRadialAction("Export project",
                                            com.nikgapps.app.presentation.ui.component.AppIcons.Info, onClick = { invoked++ })),
                                        useRadial = false, onDismiss = { projectMenu = false })
                                }
                                ProjectPackageCard(fixture("youtube", "YouTube"), RegistryDeviceStatus(false),
                                    selected = false, onOpen = { invoked++ }, onSelected = { error("Selection changed") },
                                    keepAosp = false, onKeepAospChange = {})
                            }
                        }
                    }
                } }
                for (darkMode in listOf(false, true)) for (glass in listOf(true, false)) {
                    scenario.onActivity { dark = darkMode; com.nikgapps.app.data.DisplayPrefs.useLiquidGlass = glass }
                    val suffix = "${if (glass) "glass" else "classic"}-${if (darkMode) "dark" else "light"}"
                    longPress("YouTube")
                    awaitText("Show info")
                    screenshot("package-menu-$suffix")
                    back()
                    click("More options")
                    awaitText("Logs")
                    screenshot("header-menu-$suffix")
                    back()
                    click("Project actions")
                    awaitText("Export project")
                    screenshot("project-menu-$suffix")
                    back()
                    awaitText("YouTube")
                    assertEquals("Opening and dismissing menus must not run their actions", 0, invoked)
                }
            }
        } finally { com.nikgapps.app.data.DisplayPrefs.useLiquidGlass = originalGlass }
    }

    @Test fun onlyRemovalPackagesOfferStockActionsAndRequireConfirmation() {
        check(instrumentation.targetContext.packageName == "com.nikgapps.review.debug")
        var keep by mutableStateOf(false)
        var contactsProject by mutableStateOf(BuildProject(name = "Test",
            androidVersion = AndroidVersion.ANDROID_17, architecture = Architecture.ARM64))
        var changes = 0
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Choose your apps")
                            ProjectPackageCard(fixture("calendar", "Google Calendar"), RegistryDeviceStatus(false),
                                selected = true, onOpen = {}, onSelected = {}, keepAosp = keep,
                                onKeepAospChange = { keep = it; changes++ },
                                stockRemovals = listOf("Calendar", "Etar", "SimpleCalendar"))
                            ProjectPackageCard(fixture("youtube", "YouTube"), RegistryDeviceStatus(false),
                                selected = true, onOpen = {}, onSelected = {}, keepAosp = false,
                                onKeepAospChange = { error("No removals") })
                            ProjectPackageCard(fixture("contacts", "Google Contacts"), RegistryDeviceStatus(false),
                                selected = "contacts" in contactsProject.selectedAppIds, onOpen = {}, onSelected = {},
                                keepAosp = "contacts" in contactsProject.keepAospCounterparts,
                                onKeepAospChange = { contactsProject = contactsProject.withStockAppPreference("contacts", "contacts", it) },
                                stockRemovals = listOf("Contacts"))
                        }
                    }
                }
            } }
            longPress("YouTube")
            awaitText("Show info")
            assertNull(find { it.text?.toString() == "Keep Stock app" })
            back()
            longPress("Google Contacts")
            click("Keep Stock app")
            awaitText("Select & keep")
            assertTrue(contactsProject.selectedAppIds.isEmpty())
            click("Cancel")
            assertTrue(contactsProject.selectedAppIds.isEmpty())
            longPress("Google Calendar")
            click("Keep Stock app")
            listOf("Calendar", "Etar", "SimpleCalendar").forEach(::awaitText)
            assertFalse(keep)
            assertEquals(0, changes)
            screenshot("keep-stock-confirmation-dark")
            click("Cancel")
            assertEquals(0, changes)
            longPress("Google Calendar")
            click("Keep Stock app")
            click("Keep apps")
            assertTrue(keep)
            assertEquals(1, changes)
            longPress("Google Calendar")
            click("Delete Stock app")
            awaitText("Remove stock apps?")
            assertTrue(keep)
            screenshot("remove-stock-confirmation-dark")
            click("Remove apps")
            assertFalse(keep)
            assertEquals(2, changes)
            longPress("Google Contacts")
            click("Keep Stock app")
            screenshot("select-and-keep-stock-dark")
            click("Select & keep")
            assertEquals(setOf("contacts"), contactsProject.selectedAppIds)
            assertEquals("contacts", contactsProject.selectedPackageAppSets["contacts"])
            assertEquals(setOf("contacts"), contactsProject.keepAospCounterparts)
        }
    }

    @Test fun longTargetListScrollsAndKeepsActionsReachableInLightTheme() {
        check(instrumentation.targetContext.packageName == "com.nikgapps.review.debug")
        var confirmed = false
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    StockAppRemovalDialog("Android System Intelligence",
                        (1..40).map { "DevicePersonalizationPrebuiltPixel$it" }, keep = true,
                        onDismiss = {}, onConfirm = { confirmed = true })
                }
            } }
            awaitText("Keep apps")
            val scrollable = await { it.isScrollable }
            assertTrue(scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            instrumentation.waitForIdleSync()
            screenshot("keep-stock-long-list-light")
            click("Keep apps")
            assertTrue(confirmed)
        }
    }

    private fun fixture(id: String, name: String) = CatalogPackage(id, name, true, false,
        emptyList(), emptyMap(), emptyMap())

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

    private fun await(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (true) {
            find(matches)?.let { return it }
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Stock-app control not found" }
            Thread.sleep(25)
        }
    }

    private fun awaitText(text: String) = await { it.text?.toString() == text }

    private fun click(text: String) {
        var node = awaitText(text)
        while (!node.isClickable && node.parent != null) node = requireNotNull(node.parent)
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun longPress(name: String) {
        var node = await { it.text?.toString()?.lineSequence()?.any { line -> line == name } == true }
        while (!node.isLongClickable && node.parent != null) node = requireNotNull(node.parent)
        check(node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        instrumentation.waitForIdleSync()
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(400)
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
}
