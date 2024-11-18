package com.nikgapps.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.model.ThemePreference

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme()

@Composable
fun SystemTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val manager = globalClass.preferencesManager
    val darkTheme: Boolean = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (manager.displayPrefs.theme == ThemePreference.SYSTEM.ordinal) {
            isSystemInDarkTheme()
        } else manager.displayPrefs.theme == ThemePreference.DARK.ordinal
    } else {
        manager.displayPrefs.theme == ThemePreference.DARK.ordinal
    }

    fun getTheme(): ColorScheme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (manager.displayPrefs.theme) {
                ThemePreference.LIGHT.ordinal -> dynamicLightColorScheme(context)
                ThemePreference.DARK.ordinal -> dynamicDarkColorScheme(context)
                else -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                    context
                )
            }
        } else {
            when (manager.displayPrefs.theme) {
                ThemePreference.LIGHT.ordinal -> LightColorScheme
                ThemePreference.DARK.ordinal -> DarkColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    var colorScheme by remember {
        mutableStateOf(getTheme())
    }

    LaunchedEffect(manager.displayPrefs.theme) {
        colorScheme = getTheme()
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}