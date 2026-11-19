package com.nikgapps

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.data.ThemePreference
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.ui.component.StartupLogoScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StartupAppearanceInstrumentedTest {
    @Test fun nativeStartingWindowAndSystemSplashAreBlack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val theme = ContextThemeWrapper(context, R.style.Theme_NikGapps)
        val attributes = buildList {
            add(android.R.attr.windowBackground)
            if (Build.VERSION.SDK_INT >= 31) add(android.R.attr.windowSplashScreenBackground)
        }
        val values = theme.obtainStyledAttributes(attributes.toIntArray())
        try {
            attributes.indices.forEach { index ->
                assertEquals("Launch background attribute ${attributes[index]}",
                    Color.BLACK, values.getColor(index, Color.MAGENTA))
            }
        } finally { values.recycle() }
    }

    @Test fun loadingStaysBlackAndRestoresSystemBarsAfterOverlappingChecks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.nikgapps.review.debug")
        val originalTheme = DisplayPrefs.theme
        val overlays = mutableIntStateOf(2)
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    NikGappsTheme {
                        Box(Modifier.fillMaxSize()) {
                            Surface(Modifier.fillMaxSize()) {}
                            if (overlays.intValue >= 1) StartupLogoScreen()
                            if (overlays.intValue >= 2) StartupLogoScreen()
                        }
                    }
                } }
                for (theme in listOf(ThemePreference.LIGHT, ThemePreference.DARK)) {
                    scenario.onActivity { DisplayPrefs.theme = theme.ordinal; overlays.intValue = 2 }
                    instrumentation.waitForIdleSync()
                    for (count in listOf(2, 1, 0)) {
                        scenario.onActivity { overlays.intValue = count }
                        instrumentation.waitForIdleSync()
                        // Disposing a loading overlay schedules the theme's bar update next frame.
                        val expectDarkIcons = count == 0 && theme == ThemePreference.LIGHT
                        val deadline = SystemClock.uptimeMillis() + 3000
                        var barsRestored = false
                        while (!barsRestored && SystemClock.uptimeMillis() < deadline) {
                            scenario.onActivity { activity ->
                                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                barsRestored = controller.isAppearanceLightStatusBars == expectDarkIcons &&
                                    controller.isAppearanceLightNavigationBars == expectDarkIcons
                            }
                            if (!barsRestored) SystemClock.sleep(25)
                        }
                        scenario.onActivity { activity ->
                            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                            assertEquals("Status icons for $theme, $count checks", expectDarkIcons,
                                controller.isAppearanceLightStatusBars)
                            assertEquals("Navigation icons for $theme, $count checks", expectDarkIcons,
                                controller.isAppearanceLightNavigationBars)
                        }
                        if (count > 0) {
                            // Wait for the committed frame to reach the display before screen capture.
                            SystemClock.sleep(100)
                            val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                            try {
                                assertEquals("Loading background for $theme, $count checks", Color.BLACK,
                                    image.getPixel(image.width / 4, image.height / 4))
                                if (count == 1) File(context.getExternalFilesDir(null), "startup-${theme.name.lowercase()}.png")
                                    .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            } finally { image.recycle() }
                        }
                    }
                }
            }
        } finally { DisplayPrefs.theme = originalTheme }
    }
}
