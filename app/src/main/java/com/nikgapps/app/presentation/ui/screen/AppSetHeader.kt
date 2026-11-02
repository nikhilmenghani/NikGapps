package com.nikgapps.app.presentation.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun AppSetHeader(name: String, packageCount: Int, selectedCount: Int,
    expanded: Boolean, enabled: Boolean, onToggleExpanded: () -> Unit, onToggleSelection: () -> Unit) {
    val border by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = .45f)
        else MaterialTheme.colorScheme.outlineVariant, label = "AppSet outline")
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "AppSet expand")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, border)) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button,
            onClickLabel = if (expanded) "Collapse $name" else "Expand $name",
            onClick = onToggleExpanded).padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.GridView, null, Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = if (selectedCount > 0) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (selectedCount > 0) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text("$selectedCount / $packageCount", Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("selected", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TriStateCheckbox(state = when {
                selectedCount == 0 -> ToggleableState.Off
                selectedCount == packageCount -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }, enabled = enabled, onClick = onToggleSelection)
            IconButton(onClick = onToggleExpanded) {
                Icon(Icons.Default.ExpandMore, if (expanded) "Collapse $name" else "Expand $name",
                    Modifier.rotate(rotation))
            }
        }
    }
}
