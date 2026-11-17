package com.nikgapps.app.presentation.ui.component

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.nikgapps.app.data.DisplayPrefs

private class GoogleLightField(val size: Size, val base: Color, dark: Boolean) {
    private val ground = lerp(base, if (dark) Color(0xFF102A24) else Color(0xFFF0F6F2), if (dark) .55f else .35f)
    private fun glow(color: Color, alpha: Float, x: Float, y: Float, radius: Float) =
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width * x, size.height * y), radius = (size.width * radius).coerceAtLeast(1f))
    private val lights = listOf(
        glow(Color(0xFF4285F4), if (dark) .24f else .19f, -.18f, .10f, 1.15f),
        glow(Color(0xFFFBBC05), if (dark) .48f else .27f, .68f, -.02f, 1.12f),
        glow(Color(0xFFEA4335), if (dark) .15f else .10f, 1.10f, .38f, .85f),
        glow(Color(0xFF34A853), if (dark) .23f else .17f, -.15f, .72f, 1.35f),
        glow(Color(0xFF4285F4), if (dark) .14f else .10f, 1.10f, .98f, 1.15f))
    fun draw(scope: DrawScope) = with(scope) {
        drawRect(ground, size = this@GoogleLightField.size)
        lights.forEach { drawRect(it, size = this@GoogleLightField.size) }
    }
}

private data class GlassBackdrop(val field: GoogleLightField, val origin: Offset)
private val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Shared scene coordinates let each tile sample the light field at its actual scroll position. */
@Composable
internal fun ProjectsBackdrop(enabled: Boolean = true, content: @Composable () -> Unit) {
    val glassEnabled = enabled && DisplayPrefs.useLiquidGlass
    val colors = MaterialTheme.colorScheme
    var size by remember { mutableStateOf(Size.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val field = remember(size, colors.background, glassEnabled) {
        if (glassEnabled) GoogleLightField(size, colors.background, colors.background.luminance() < .5f) else null
    }
    CompositionLocalProvider(LocalGlassBackdrop provides field?.let { GlassBackdrop(it, origin) }) {
        Box(Modifier.fillMaxSize().onSizeChanged { size = it.toSize() }
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawWithContent {
                if (field != null) field.draw(this) else drawRect(colors.background)
                drawContent()
            }) { content() }
    }
}

@Composable
internal fun ProjectGlassSurface(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val glassEnabled = DisplayPrefs.useLiquidGlass
    val dark = colors.background.luminance() < .5f
    val backdrop = LocalGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val blurRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 22.dp.toPx() }
    val frost = colors.surface.copy(alpha = if (dark) .20f else .36f)
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
    Surface(modifier = modifier.onGloballyPositioned { position = it.positionInRoot() },
        shape = shape, color = if (glassEnabled) Color.Transparent else colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        border = if (glassEnabled) BorderStroke(.75.dp, rim) else BorderStroke(1.dp, colors.outlineVariant),
        tonalElevation = if (glassEnabled) 0.dp else 2.dp, shadowElevation = if (glassEnabled) 4.dp else 0.dp) {
        Box {
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
            Box { content() }
        }
    }
}
