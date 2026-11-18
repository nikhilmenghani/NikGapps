package com.nikgapps.app.presentation.ui.component

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.data.BackgroundPrefs
import com.nikgapps.app.data.GlassBackground
import com.nikgapps.app.presentation.theme.palette

private class GlassLightField(val size: Size, base: Color, dark: Boolean, background: GlassBackground) {
    private val palette = background.palette
    private val ground = lerp(base, if (dark) palette.darkGround else palette.lightGround, if (dark) .55f else .35f)
    private fun glow(color: Color, alpha: Float, x: Float, y: Float, radius: Float) =
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width * x, size.height * y), radius = (size.width * radius).coerceAtLeast(1f))
    private val lights = listOf(
        glow(palette.lights[0], if (dark) .24f else .19f, -.18f, .10f, 1.15f),
        glow(palette.lights[1], if (dark) .48f else .27f, .68f, -.02f, 1.12f),
        glow(palette.lights[2], if (dark) .15f else .10f, 1.10f, .38f, .85f),
        glow(palette.lights[3], if (dark) .23f else .17f, -.15f, .72f, 1.35f),
        glow(palette.lights[4], if (dark) .14f else .10f, 1.10f, .98f, 1.15f))
    fun draw(scope: DrawScope) = with(scope) {
        drawRect(ground, size = this@GlassLightField.size)
        lights.forEach { drawRect(it, size = this@GlassLightField.size) }
    }
}

private data class GlassBackdrop(val field: GlassLightField, val origin: Offset)
private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
internal val LocalGlassScene = staticCompositionLocalOf { false }

/** Shared scene coordinates let each tile sample the light field at its actual scroll position. */
@Composable
internal fun ProjectsBackdrop(modifier: Modifier = Modifier, enabled: Boolean = true,
    background: GlassBackground = BackgroundPrefs.selected, content: @Composable () -> Unit) {
    val glassEnabled = enabled && DisplayPrefs.useLiquidGlass
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val screenLocation = remember(view) { IntArray(2) }
    var size by remember { mutableStateOf(Size.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val field = remember(size, colors.background, glassEnabled, background) {
        if (glassEnabled) GlassLightField(size, colors.background, colors.background.luminance() < .5f, background) else null
    }
    CompositionLocalProvider(LocalGlassScene provides true,
        LocalGlassBackdrop provides field?.let { GlassBackdrop(it, origin) }) {
        Box(modifier.fillMaxSize().onSizeChanged { size = it.toSize() }
            .onGloballyPositioned {
                view.getLocationOnScreen(screenLocation)
                origin = it.positionInRoot() + Offset(screenLocation[0].toFloat(), screenLocation[1].toFloat())
            }
            .drawWithContent {
                if (field != null) field.draw(this) else drawRect(colors.background)
                drawContent()
            }) { content() }
    }
}

/** Thumbnails use the same renderer as the actual app backdrop. */
@Composable
internal fun BackgroundSwatch(background: GlassBackground, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    Box(modifier.drawWithCache {
        val field = GlassLightField(size, base, base.luminance() < .5f, background)
        onDrawBehind { field.draw(this) }
    })
}

@Composable
internal fun ProjectGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    classicColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    classicBorder: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    classicTonalElevation: Dp = 2.dp,
    classicShadowElevation: Dp = 0.dp,
    glass: Boolean = true,
    readable: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val screenLocation = remember(view) { IntArray(2) }
    val glassEnabled = glass && DisplayPrefs.useLiquidGlass
    val dark = colors.background.luminance() < .5f
    val backdrop = LocalGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val blurRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 22.dp.toPx() }
    val frost = colors.surface.copy(alpha = if (readable) {
        if (dark) .55f else .72f
    } else if (dark) .20f else .36f)
    val rim = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) .22f else .68f),
        .28f to Color.White.copy(alpha = if (dark) .06f else .22f),
        .68f to Color.White.copy(alpha = if (dark) .025f else .10f),
        1f to Color.White.copy(alpha = if (dark) .10f else .34f))
    val reflection = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) .055f else .28f),
        .22f to Color.White.copy(alpha = if (dark) .015f else .08f),
        .55f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (dark) .065f else .025f))
    val surfaceModifier = modifier.onGloballyPositioned {
        view.getLocationOnScreen(screenLocation)
        position = it.positionInRoot() + Offset(screenLocation[0].toFloat(), screenLocation[1].toFloat())
    }
    val surfaceColor = if (glassEnabled) Color.Transparent else classicColor
    val surfaceBorder = if (glassEnabled) BorderStroke(.75.dp, rim) else classicBorder
    val surfaceTonalElevation = if (glassEnabled) 0.dp else classicTonalElevation
    val surfaceShadowElevation = if (glassEnabled) 4.dp else classicShadowElevation
    val layers: @Composable () -> Unit = {
        Box(propagateMinConstraints = true) {
            if (glassEnabled) {
            // Only the sampled background is blurred; labels, icons and touch targets are a separate layer.
            Box(Modifier.matchParentSize().graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = BlurEffect(blurRadius, blurRadius, TileMode.Clamp)
                }
            }.drawWithCache {
                onDrawBehind {
                    backdrop?.let {
                        val offset = position - it.origin
                        translate(-offset.x, -offset.y) {
                            scale(1.025f, pivot = offset + center) { it.field.draw(this) }
                        }
                    }
                }
            })
            Box(Modifier.matchParentSize().background(frost).background(reflection))
            }
            Box(propagateMinConstraints = true) { content() }
        }
    }
    // The chosen Surface overload stays stable when the appearance preference changes.
    if (onClick == null) Surface(modifier = surfaceModifier, shape = shape, color = surfaceColor,
        contentColor = contentColor, border = surfaceBorder, tonalElevation = surfaceTonalElevation,
        shadowElevation = surfaceShadowElevation, content = layers)
    else Surface(onClick = onClick, enabled = enabled, modifier = surfaceModifier, shape = shape,
        color = surfaceColor, contentColor = contentColor, border = surfaceBorder,
        tonalElevation = surfaceTonalElevation, shadowElevation = surfaceShadowElevation, content = layers)
}
