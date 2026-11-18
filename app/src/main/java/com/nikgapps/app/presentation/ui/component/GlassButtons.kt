package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.DisplayPrefs

@Composable
private fun glassButtonPadding() = if (DisplayPrefs.useLiquidGlass)
    PaddingValues(horizontal = 12.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding

@Composable
internal fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = glassButtonPadding(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = GlassActionButton(onClick, modifier, enabled, shape, colors, elevation, border,
    contentPadding, interactionSource, primary = true, content)

@Composable
internal fun GlassTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = glassButtonPadding(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = GlassActionButton(onClick, modifier, enabled, shape, colors, elevation, border,
    contentPadding, interactionSource, primary = false, content)

/** Keep native button semantics, ripple and disabled handling in both appearances. */
@Composable
private fun GlassActionButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: ButtonColors,
    elevation: ButtonElevation?,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    primary: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val glass = DisplayPrefs.useLiquidGlass
    val glassColors = ButtonDefaults.buttonColors(
        containerColor = colors.containerColor.copy(alpha = if (primary) .12f else .06f),
        contentColor = if (primary) colors.containerColor else MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .10f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
    )
    GlassSurface(modifier.heightIn(min = 48.dp), shape = shape, color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface) {
        Button(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp),
            enabled = enabled, shape = shape, colors = if (glass) glassColors else colors,
            elevation = if (glass) null else elevation, border = if (glass) null else border,
            contentPadding = contentPadding, interactionSource = interactionSource, content = content)
    }
}
