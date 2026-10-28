package com.nikgapps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.presentation.ui.screen.PackageRowContents
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Renders isolated fixtures; never changes device density, projects, or account settings. */
class PackageRowLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun installedBadgeFitsAcrossDensityWidthAndTextScale() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.nikgapps.review.debug")
        val config = mutableStateOf(Triple(1f, 1f, 320))
        val legacy = mutableStateOf(true)
        compose.setContent {
            val (density, fontScale, width) = config.value
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Surface(Modifier.requiredWidth(width.dp).testTag("package-row")) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (legacy.value) {
                                // Reproduce the previous fixed-width badge constraints.
                                Box(Modifier.width(56.dp).height(44.dp)) {
                                    Text("Installed", Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp))
                                }
                            } else PackageRowContents("GoogleServicesFramework", "com.google.android.gsf", true, true)
                        }
                    }
                }
            }
        }
        for (density in listOf(1f, 1.5f, 2f, 2.75f, 3.75f)) {
            compose.runOnIdle { config.value = Triple(density, 1f, 360) }
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("Installed", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("Density alone should not wrap the original badge", 1, layouts.single().lineCount)
        }
        compose.runOnIdle { config.value = Triple(2f, 2f, 360) }
        val legacyLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Installed", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(legacyLayouts) }
        assertTrue("Large text reproduces the original wrapping defect", legacyLayouts.single().lineCount > 1)
        compose.runOnIdle { legacy.value = false }
        for (density in listOf(1f, 1.5f, 2f, 2.75f, 3.75f)) {
            for (fontScale in listOf(1f, 1.3f, 1.6f, 2f)) {
                for (width in listOf(320, 360, 384)) {
                    compose.runOnIdle { config.value = Triple(density, fontScale, width) }
                    compose.waitForIdle()
                    val layouts = mutableListOf<TextLayoutResult>()
                    compose.onNodeWithText("Installed", useUnmergedTree = true)
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    val layout = layouts.single()
                    val label = "$density dpi multiplier, font $fontScale, width $width"
                    assertEquals(label, 1, layout.lineCount)
                    assertFalse("$label: size=${layout.size}, paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}", layout.hasVisualOverflow)
                    val badge = compose.onNodeWithText("Installed", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    val row = compose.onNodeWithTag("package-row").fetchSemanticsNode().boundsInRoot
                    assertTrue(label, badge.left >= row.left && badge.right <= row.right && badge.bottom <= row.bottom)
                    val name = compose.onNodeWithText("GoogleServicesFramework", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    assertTrue("$label: badge must not crowd package text", badge.right < name.left)
                    if (width == 360 && fontScale in listOf(1f, 2f) && density in listOf(1f, 3.75f)) {
                        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                            "package-row-density-$density-font-$fontScale.png")
                        file.outputStream().use { out ->
                            compose.onNodeWithTag("package-row").captureToImage().asAndroidBitmap()
                                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            }
        }
    }
}
