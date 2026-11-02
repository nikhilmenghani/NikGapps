package com.nikgapps.app.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "AppSet expand")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Box {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button,
            onClickLabel = if (expanded) "Collapse $name" else "Expand $name",
            onClick = onToggleExpanded).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(32.dp), shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.GridView, null, Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$selectedCount of $packageCount selected", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TriStateCheckbox(state = when {
                selectedCount == 0 -> ToggleableState.Off
                selectedCount == packageCount -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }, enabled = enabled, onClick = onToggleSelection)
            IconButton(onClick = onToggleExpanded, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ExpandMore, if (expanded) "Collapse $name" else "Expand $name",
                    Modifier.rotate(rotation))
            }
        }
        if (selectedCount > 0) Box(Modifier.matchParentSize()) {
            Box(Modifier.align(Alignment.CenterStart).width(3.dp).fillMaxHeight(.65f)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        }
        }
    }
}
