package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** Move the action pill below the title when either title or subtitle needs more room. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptiveTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actionCount: Int = 4,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleLarge
    val subtitleStyle = MaterialTheme.typography.labelSmall
    BoxWithConstraints {
        val leadingSpace = if (onBack == null) 32.dp else 72.dp
        val inlineWidth = with(density) { (maxWidth - leadingSpace - (actionCount * 48 + 20).dp).toPx() }
        val stacked = measurer.measure(title, titleStyle, softWrap = false).size.width > inlineWidth ||
            (subtitle != null && measurer.measure(subtitle, subtitleStyle, softWrap = false).size.width > inlineWidth)
        val titleWidth = with(density) { (maxWidth - leadingSpace).roundToPx().coerceAtLeast(1) }
        val titleHeight = if (stacked) with(density) {
            measurer.measure(title, titleStyle, constraints = Constraints(maxWidth = titleWidth)).size.height.toDp() +
                (subtitle?.let { measurer.measure(it, subtitleStyle,
                    constraints = Constraints(maxWidth = titleWidth)).size.height.toDp() } ?: 0.dp) + 16.dp
        }.coerceAtLeast(64.dp) else 64.dp
        Column {
            TopAppBar(title = {
                Column {
                    Text(title, style = titleStyle)
                    subtitle?.let { Text(it, style = subtitleStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }, navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }, actions = { if (!stacked) actions() }, expandedHeight = titleHeight,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent), scrollBehavior = scrollBehavior)
            if (stacked) Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
                content = actions)
        }
    }
}
