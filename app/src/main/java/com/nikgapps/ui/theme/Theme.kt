package com.nikgapps.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Define overrides for dark theme
    val darkColorOverrides = baseColorScheme.copy(
//        primary = DarkGrey, // Override primary color
//        secondary = DarkGrey, // Override secondary color
//        tertiary = DarkGrey, // Override tertiary color
//        background = DarkGrey, // Override background color
//        surface = DarkGrey, // Override surface color
//        onPrimary = Color.White, // Override onPrimary color
//        onSecondary = Color.White, // Override onSecondary color
//        onTertiary = Color.White, // Override onTertiary color
//        onBackground = Pink40, // Override onBackground color
//        onSurface = Color.White // Override onSurface color
    )

    // Define overrides for light theme
    val lightColorOverrides = baseColorScheme.copy(
//        primary = LightGrey, // Override primary color
//        secondary = LightGrey, // Override secondary color
//        tertiary = LightGrey, // Override tertiary color
//        background = LightGrey, // Override background color
//        surface = LightGrey, // Override surface color
//        onPrimary = Color.Black, // Override onPrimary color
//        onSecondary = Color.Black, // Override onSecondary color
//        onTertiary = Color.Black, // Override onTertiary color
//        onBackground = Pink40, // Override onBackground color
//        onSurface = Color.Black // Override onSurface color
    )

    // Apply the correct overrides based on the theme
    val colorScheme = if (darkTheme) darkColorOverrides else lightColorOverrides

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}