// NikGappsTheme.kt
package com.nikgapps.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.ThemePreference

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

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
    val darkTheme: Boolean = if (useDynamicColor) {
        isSystemInDarkTheme()
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (manager.displayPrefs.theme == ThemePreference.SYSTEM.ordinal) {
                isSystemInDarkTheme()
            } else manager.displayPrefs.theme == ThemePreference.DARK.ordinal
        } else {
            manager.displayPrefs.theme == ThemePreference.DARK.ordinal
        }
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Override specific colors only if dynamicColor is false
    val customColorScheme = if (!useDynamicColor) {
        colorScheme.copy(
            primary = if (darkTheme) Color(0xFF1E88E5) else Color(0xFF1976D2),
            secondary = if (darkTheme) Color(0xFF03DAC6) else Color(0xFF03DAC6),
            tertiary = if (darkTheme) Color(0xFF03DAC6) else Color(0xFF018786),
            background = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF),
            surface = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF),
            surfaceVariant = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
            onPrimary = if (darkTheme) Color.Black else Color.White,
            onSecondary = if (darkTheme) Color.Black else Color.White,
            onTertiary = if (darkTheme) Color.Black else Color.White,
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            onSurfaceVariant = if (darkTheme) Color.LightGray else Color.Black
        )
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = customColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = customColorScheme,
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

    val customColorScheme = if (!useDynamicColor) {
        colorScheme.copy(
            primary = if (darkTheme) Color(0xFF1E88E5) else Color(0xFF1976D2),
            secondary = if (darkTheme) Color(0xFF03DAC6) else Color(0xFF03DAC6),
            tertiary = if (darkTheme) Color(0xFF03DAC6) else Color(0xFF018786),
            background = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF),
            surface = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF),
            surfaceVariant = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE0E0E0),
            onPrimary = if (darkTheme) Color.Black else Color.White,
            onSecondary = if (darkTheme) Color.Black else Color.White,
            onTertiary = if (darkTheme) Color.Black else Color.White,
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            onSurfaceVariant = if (darkTheme) Color.LightGray else Color.Black
        )
    } else {
        colorScheme
    }

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography,
        content = content
    )
}

