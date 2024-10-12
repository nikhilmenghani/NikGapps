// NikGappsTheme.kt
package com.nikgapps.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.ui.screens.SharedViewModel

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
    val useDynamicColor = manager.displayPrefs.useDynamicColor
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Override specific colors only if dynamicColor is false
    val customColorScheme = if (!(useDynamicColor)) {
        colorScheme.copy(
            primary = if (darkTheme) DarkGrey else LightGrey,
            secondary = if (darkTheme) DarkGrey else LightGrey,
            tertiary = if (darkTheme) DarkGrey else LightGrey,
            background = if (darkTheme) DarkGrey else LightGrey,
            surface = if (darkTheme) DarkGrey else LightGrey,
            onPrimary = if (darkTheme) Color.White else Color.Black,
            onSecondary = if (darkTheme) Color.White else Color.Black,
            onTertiary = if (darkTheme) Color.White else Color.Black,
            onBackground = Pink40,
            onSurface = if (darkTheme) Color.White else Color.Black
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
fun NikGappsThemePreview(content: @Composable () -> Unit) {
    // Simplified version of NikGappsTheme for previewing
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

