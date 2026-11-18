package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Shared header action container for Home, package selection and diagnostics. */
@Composable
internal fun HeaderActionPill(content: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    ProjectGlassSurface(modifier = Modifier.padding(end = 12.dp), shape = CircleShape) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
        Row(Modifier.background(colors.surface.copy(alpha = if (dark) .22f else .18f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}
