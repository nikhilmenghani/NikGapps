package com.nikgapps.app.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.*

data class ProjectRadialAction(val label: String, val icon: ImageVector,
    val enabled: Boolean = true, val destructive: Boolean = false,
    val compactLabel: String = label, val onClick: () -> Unit)

@Composable
fun ProjectContextMenu(projectName: String, actions: List<ProjectRadialAction>,
    useRadial: Boolean, onDismiss: () -> Unit) {
    if (useRadial) {
        ProjectRadialMenu(projectName, actions, onDismiss)
    } else {
        DropdownMenu(expanded = true, onDismissRequest = onDismiss,
            shape = RoundedCornerShape(12.dp), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            actions.forEach { action ->
                val color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    text = { Text(action.label, color = color.copy(alpha = if (action.enabled) 1f else .38f)) },
                    leadingIcon = { Icon(action.icon, null, tint = color.copy(alpha = if (action.enabled) 1f else .38f)) },
                    enabled = action.enabled, onClick = { onDismiss(); action.onClick() })
            }
        }
    }
}

/** A tap-to-choose radial menu. Releasing the original long press never runs an action. */
@Composable
fun ProjectRadialMenu(projectName: String, actions: List<ProjectRadialAction>, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val preferredDiameter = when {
        actions.size <= 4 -> 208.dp
        actions.size == 5 -> 224.dp
        actions.size == 6 -> 248.dp
        else -> 272.dp
    }
    val diameter = with(density) {
        minOf(preferredDiameter, (minOf(window.width, window.height).toDp() * .76f).coerceAtLeast(1.dp))
    }
    val margin = with(density) { 8.dp.roundToPx() }
    val position = remember(margin) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
            layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset = IntOffset(
            (anchorBounds.left - popupContentSize.width / 2).coerceIn(margin, maxOf(margin, windowSize.width - popupContentSize.width - margin)),
            (anchorBounds.top - popupContentSize.height / 2).coerceIn(margin, maxOf(margin, windowSize.height - popupContentSize.height - margin)))
    } }
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { entered = true }
        val progress by animateFloatAsState(if (entered) 1f else 0f, tween(160), label = "Project menu entrance")
        Column(Modifier.width(diameter).graphicsLayer {
            alpha = progress; scaleX = .92f + .08f * progress; scaleY = scaleX
        }, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Text(projectName, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
        Surface(Modifier.size(diameter), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 8.dp) {
            Box(Modifier.fillMaxSize().padding(5.dp)) {
                val wheelSize = diameter - 10.dp
                val sweep = 360f / actions.size
                actions.forEachIndexed { index, action ->
                    val angle = -90f + index * sweep
                    val shape = remember(index, actions.size) { GenericShape { size, _ ->
                        val center = size.width / 2f
                        val outer = center
                        val inner = outer * .40f
                        val start = angle - sweep / 2f + 1.5f
                        val length = sweep - 3f
                        arcTo(Rect(0f, 0f, size.width, size.height), start, length, true)
                        val end = Math.toRadians((start + length).toDouble())
                        lineTo(center + inner * cos(end).toFloat(), center + inner * sin(end).toFloat())
                        arcTo(Rect(center - inner, center - inner, center + inner, center + inner), start + length, -length, false)
                        close()
                    } }
                    val foreground = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                    Surface(onClick = { onDismiss(); action.onClick() }, enabled = action.enabled,
                        modifier = Modifier.fillMaxSize(), shape = shape,
                        color = if (action.destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = foreground.copy(alpha = if (action.enabled) 1f else .38f)) {
                        Box(Modifier.fillMaxSize()) {
                            val radians = Math.toRadians(angle.toDouble())
                            val radius = wheelSize * .35f
                            Column(Modifier.absoluteOffset(
                                x = wheelSize / 2 + radius * cos(radians).toFloat() - 32.dp,
                                y = wheelSize / 2 + radius * sin(radians).toFloat() - 32.dp
                            ).width(64.dp).height(64.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(action.icon, null, Modifier.size(18.dp))
                                Spacer(Modifier.height(3.dp))
                                Text(action.compactLabel, style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp, lineHeight = 13.sp),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Surface(Modifier.align(Alignment.Center).size(wheelSize * .37f), shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, "Close project menu")
                        }
                    }
                }
            }
        }
        }
    }
}
