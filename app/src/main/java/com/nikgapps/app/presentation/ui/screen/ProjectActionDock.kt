package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ProjectDockAction(val label: String, val icon: ImageVector,
    val enabled: Boolean = true, val highlighted: Boolean = false, val onClick: () -> Unit)

/** A shared floating dock: additional actions do not need another button row. */
@Composable
fun ProjectActionDock(actions: List<ProjectDockAction>, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            actions.forEach { action ->
                val content = if (action.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                Column(Modifier.weight(1f).fillMaxHeight()
                    .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
                    .padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = if (action.highlighted && action.enabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (action.highlighted && action.enabled) MaterialTheme.colorScheme.onPrimaryContainer else content) {
                        Box(Modifier.width(54.dp).height(28.dp), contentAlignment = Alignment.Center) {
                            Icon(action.icon, null, Modifier.size(21.dp))
                        }
                    }
                    Text(action.label, color = content, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
