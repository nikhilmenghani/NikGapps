package com.nikgapps

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.*
import com.nikgapps.app.presentation.ui.component.ProjectsBackdrop
import com.nikgapps.app.presentation.ui.component.LiquidGlassPreference
import com.nikgapps.app.presentation.ui.component.LIQUID_GLASS_DESCRIPTION
import com.nikgapps.app.presentation.ui.screen.ProjectCard
import com.nikgapps.app.registry.ProjectUpdate
import com.nikgapps.app.registry.ProjectUpdateKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import java.io.File

class ProjectGlassInstrumentedTest {
    @Test fun projectCardsRemainReadableAndOpenInBothStylesAndThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val originalGlassPreference = DisplayPrefs.useLiquidGlass
        var dark by mutableStateOf(true)
        var opened = 0
        val projects = listOf("Core", "Daily essentials", "Android Auto", "Custom build").mapIndexed { index, name ->
            BuildProject(name = name, androidVersion = AndroidVersion.ANDROID_17,
                architecture = Architecture.ARM64, createdAt = 1794787200000,
                selectedAppIds = (0..index + 2).map { "app$it" }.toSet())
        }
        try {
        DisplayPrefs.useLiquidGlass = true
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                SideEffect {
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    ProjectsBackdrop {
                        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Projects", style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground)
                            LiquidGlassPreference()
                            projects.forEachIndexed { index, project ->
                                ProjectCard(project, onMenuVisibilityChanged = {},
                                    unavailable = if (index == 3) setOf("app0") else emptySet(),
                                    update = if (index == 1) ProjectUpdate(ProjectUpdateKind.AVAILABLE, "next", "Update available") else null,
                                    onDismissUpdate = {}, onDismissAllUpdates = {}, onExport = {}, onOpen = { opened++ },
                                    onBuild = {}, onShareZip = null, onDuplicate = {}, onEdit = {}, onDelete = {})
                            }
                        }
                    }
                }
            } }
            instrumentation.waitForIdleSync()
            assertFalse(findNode { it.text?.toString() == LIQUID_GLASS_DESCRIPTION } != null)
            click { it.contentDescription?.toString() == "About liquid glass" }
            assertTrue(DisplayPrefs.useLiquidGlass)
            awaitNode { it.text?.toString() == LIQUID_GLASS_DESCRIPTION }
            click { it.text?.toString() == "Got it" }
            awaitNode { it.contentDescription?.toString() == "About liquid glass" }
            click { it.text?.toString() == "Liquid glass" }
            assertFalse(DisplayPrefs.useLiquidGlass)
            val persisted = runBlocking { context.dataStore.data.first()[booleanPreferencesKey("useLiquidGlass")] }
            assertEquals(false, persisted)
            for (darkTheme in listOf(true, false)) {
              for (glassEnabled in listOf(true, false)) {
                scenario.onActivity { dark = darkTheme; DisplayPrefs.useLiquidGlass = glassEnabled }
                instrumentation.waitForIdleSync()
                Thread.sleep(400)
                val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                File(context.getExternalFilesDir(null), "${if (glassEnabled) "glass" else "classic"}-project-${if (darkTheme) "dark" else "light"}.png")
                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                image.recycle()
                val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
                instrumentation.uiAutomation.rootInActiveWindow?.let { pending.add(it) }
                var core: AccessibilityNodeInfo? = null
                while (pending.isNotEmpty() && core == null) {
                    val node = pending.removeFirst()
                    if (node.text?.toString() == "Core") core = node
                    else repeat(node.childCount) { node.getChild(it)?.let(pending::add) }
                }
                var target = requireNotNull(core)
                while (!target.isClickable && target.parent != null) target = requireNotNull(target.parent)
                check(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                instrumentation.waitForIdleSync()
              }
            }
            assertEquals(4, opened)
        }
        } finally { DisplayPrefs.useLiquidGlass = originalGlassPreference }
    }

    private fun findNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(pending::add)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (matches(node)) return node
            repeat(node.childCount) { node.getChild(it)?.let(pending::add) }
        }
        return null
    }

    private fun awaitNode(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (true) {
            findNode(matches)?.let { return it }
            check(android.os.SystemClock.uptimeMillis() < deadline) { "Preview control not found" }
            Thread.sleep(25)
        }
    }

    private fun click(matches: (AccessibilityNodeInfo) -> Boolean) {
        var node = awaitNode(matches)
        while (!node.isClickable && node.parent != null) node = requireNotNull(node.parent)
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
