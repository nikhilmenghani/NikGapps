package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.DisplayPrefs

/** Add a scene only for standalone activities/previews; navigation shares one scene. */
@Composable
internal fun GlassPage(content: @Composable () -> Unit) {
    if (LocalGlassScene.current) content() else ProjectsBackdrop(content = content)
}

/** Keeps the original Material colors and elevations when Liquid glass is disabled. */
@Composable
internal fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    glass: Boolean = true,
    readable: Boolean = false,
    content: @Composable () -> Unit
) {
    ProjectGlassSurface(modifier = modifier, shape = shape, classicColor = color,
        contentColor = contentColor, classicBorder = border,
        classicTonalElevation = tonalElevation, classicShadowElevation = shadowElevation,
        onClick = onClick, enabled = enabled, glass = glass, readable = readable, content = content)
}

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(color),
    elevation: Dp = 1.dp,
    border: BorderStroke? = null,
    glass: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(modifier, shape, color,
        if (glass && DisplayPrefs.useLiquidGlass) MaterialTheme.colorScheme.onSurface else contentColor, shadowElevation = elevation,
        border = border, glass = glass) { Column(content = content) }
}

/** Full-screen dialogs need their own scene coordinates rather than their parent window's. */
@Composable
internal fun GlassScreenSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    ProjectsBackdrop {
        Surface(modifier, color = if (DisplayPrefs.useLiquidGlass) Color.Transparent else color,
            contentColor = contentColorFor(color), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    GlassPage {
        Scaffold(modifier, topBar = topBar, bottomBar = bottomBar,
            containerColor = if (DisplayPrefs.useLiquidGlass) Color.Transparent else containerColor,
            contentColor = contentColorFor(containerColor),
            contentWindowInsets = contentWindowInsets, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun glassTopAppBarColors(): TopAppBarColors = if (DisplayPrefs.useLiquidGlass)
    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
else TopAppBarDefaults.topAppBarColors()
