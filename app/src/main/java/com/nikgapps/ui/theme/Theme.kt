// Theme.kt
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
import com.nikgapps.screens.SharedViewModel

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun NikGappsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: SharedViewModel,
    content: @Composable () -> Unit
) {
    val dynamicColor by viewModel.useDynamicColor.collectAsState()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Override specific colors only if dynamicColor is false
    val customColorScheme = if (!dynamicColor) {
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
