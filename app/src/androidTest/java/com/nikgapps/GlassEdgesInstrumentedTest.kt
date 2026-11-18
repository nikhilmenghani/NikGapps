package com.nikgapps

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.*
import com.nikgapps.app.presentation.theme.withGlassAccent
import com.nikgapps.app.presentation.ui.component.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Pixel regression: the bottom/right silhouette must remain visible on every backdrop. */
class GlassEdgesInstrumentedTest {
    @Test fun bottomAndRightEdgesRemainVisibleAcrossBackgroundsAndThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val originalGlass = DisplayPrefs.useLiquidGlass
        val originalBackground = BackgroundPrefs.selected
        val bounds = mutableMapOf<String, Rect>()
        var dark by mutableStateOf(false)
        val density = context.resources.displayMetrics.density
        try {
            DisplayPrefs.useLiquidGlass = true
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val view = LocalView.current
                    fun probe(id: String) = Modifier.onGloballyPositioned { coordinates ->
                        val origin = IntArray(2).also(view::getLocationOnScreen)
                        val offset = coordinates.positionInRoot()
                        val x = (origin[0] + offset.x).roundToInt()
                        val y = (origin[1] + offset.y).roundToInt()
                        bounds[id] = Rect(x, y, x + coordinates.size.width, y + coordinates.size.height)
                    }
                    val base = if (dark) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = base.withGlassAccent(dark, BackgroundPrefs.selected)) {
                        ProjectsBackdrop {
                            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text("Glass component checks", style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground)
                                GlassSurface(Modifier.fillMaxWidth().height(80.dp).then(probe("card")),
                                    shape = RoundedCornerShape(18.dp)) { Text("Project and package cards", Modifier.padding(16.dp)) }
                                ProjectGlassSurface(Modifier.fillMaxWidth().height(52.dp).then(probe("pill")),
                                    shape = CircleShape) { Text("Header and navigation pills", Modifier.padding(16.dp)) }
                                GlassSurface(Modifier.fillMaxWidth().height(64.dp).then(probe("field")),
                                    shape = RoundedCornerShape(16.dp)) {
                                    TextField("Search packages", {}, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent))
                                }
                                GlassSurface(Modifier.fillMaxWidth().height(80.dp).then(probe("readable")),
                                    shape = RoundedCornerShape(18.dp), readable = true) {
                                    Text("Readable panels and logs", Modifier.padding(16.dp))
                                }
                                GlassButton({}, Modifier.fillMaxWidth().then(probe("primary"))) { Text("Primary action") }
                                GlassTonalButton({}, Modifier.fillMaxWidth().then(probe("secondary"))) { Text("Secondary action") }
                                GlassButton({}, Modifier.fillMaxWidth().then(probe("disabled")), enabled = false) { Text("Disabled action") }
                            }
                        }
                    }
                } }
                for (darkMode in listOf(false, true)) for (background in GlassBackground.entries) {
                    scenario.onActivity { dark = darkMode; BackgroundPrefs.selected = background }
                    instrumentation.waitForIdleSync()
                    Thread.sleep(450)
                    val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                    try {
                        val filename = "glass-edges-${background.id}-${if (darkMode) "dark" else "light"}.png"
                        File(context.getExternalFilesDir(null), filename).outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        assertTrue("Every surface type must be rendered", bounds.size == 7)
                        for ((id, rect) in bounds) {
                            assertTrue("$id must fit on screen", rect.bottom + 12 * density < bitmap.height)
                            for (bottom in listOf(false, true)) {
                                val x = if (bottom) rect.centerX() else rect.right
                                val y = if (bottom) rect.bottom else rect.centerY()
                                val outside = (12 * density).roundToInt()
                                val reference = bitmap.getPixel(if (bottom) x else x + outside, if (bottom) y + outside else y)
                                val contrast = (1..ceil(density * 1.4).toInt()).maxOf { inset ->
                                    val pixel = bitmap.getPixel(if (bottom) x else x - inset, if (bottom) y - inset else y)
                                    ColorUtils.calculateContrast(pixel, reference)
                                }
                                assertTrue("$id ${if (bottom) "bottom" else "right"} edge faded on ${background.id}, dark=$darkMode (contrast=$contrast)",
                                    contrast >= 1.10)
                            }
                        }
                    } finally { bitmap.recycle() }
                }
            }
        } finally {
            DisplayPrefs.useLiquidGlass = originalGlass
            BackgroundPrefs.selected = originalBackground
        }
    }
}
