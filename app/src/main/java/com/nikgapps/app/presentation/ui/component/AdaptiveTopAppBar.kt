package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/** Keep toolbar actions aligned; give longer headings their own full-width row. */
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
        val heading: @Composable () -> Unit = {
            Column {
                Text(title, style = titleStyle)
                subtitle?.let { Text(it, style = subtitleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Column {
            TopAppBar(title = { if (!stacked) heading() }, navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }, actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent), scrollBehavior = scrollBehavior)
            if (stacked) Box(Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) { heading() }
        }
    }
}
