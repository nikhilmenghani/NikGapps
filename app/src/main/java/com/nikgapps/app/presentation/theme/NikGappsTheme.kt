// NikGappsTheme.kt
package com.nikgapps.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.ThemePreference
import com.nikgapps.app.data.BackgroundPrefs
import com.nikgapps.app.data.GlassBackground

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB), onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858), onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD7BDE4), onTertiary = Color(0xFF3B2947),
    tertiaryContainer = Color(0xFF523F5F), onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF101418), onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF101418), onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199), outlineVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0B0F13), surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1C2024), surfaceContainerHigh = Color(0xFF262A2F),
    surfaceContainerHighest = Color(0xFF31353A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F7), onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3DAFF), onTertiaryContainer = Color(0xFF251431),
    background = Color(0xFFF8F9FF), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB), onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F), outlineVariant = Color(0xFFC3C7CF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF4), surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8)
)

/** One coordinated accent family for every control; theme surfaces and status colors remain intact. */
internal fun ColorScheme.withGlassAccent(dark: Boolean,
    background: GlassBackground = GlassBackground.GOOGLE): ColorScheme {
    val palette = background.palette
    val accent = if (dark) palette.darkAccent else palette.lightAccent
    return copy(
        primary = accent.primary, onPrimary = accent.onPrimary,
        primaryContainer = accent.container, onPrimaryContainer = accent.onContainer,
        secondary = lerp(accent.primary, onSurface, .55f), onSecondary = accent.onPrimary,
        secondaryContainer = lerp(accent.container, surface, .35f),
        onSecondaryContainer = lerp(accent.onContainer, onSurface, .5f),
        surfaceTint = accent.primary,
        inversePrimary = if (dark) palette.lightAccent.primary else palette.darkAccent.primary
    )
}

fun Color.applyOpacity(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.62f)
}

@Composable
fun NikGappsTheme(
    content: @Composable () -> Unit
) {
    val manager = globalClass.preferencesManager
    val context = LocalContext.current
    val useDynamicColor = manager.displayPrefs.useDynamicColor
    val darkTheme = when (manager.displayPrefs.theme) {
        ThemePreference.LIGHT.ordinal -> false
        ThemePreference.DARK.ordinal -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (manager.displayPrefs.useLiquidGlass)
            colorScheme.withGlassAccent(darkTheme, BackgroundPrefs.selected) else colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun NikGappsThemePreview(useDynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

