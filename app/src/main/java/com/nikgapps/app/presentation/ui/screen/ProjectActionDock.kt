package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ProjectDockAction(val label: String, val icon: ImageVector,
    val enabled: Boolean = true, val configured: Boolean = false, val onClick: () -> Unit)

/** Independent action buttons, not selectable navigation destinations. */
@Composable
fun ProjectActionDock(actions: List<ProjectDockAction>, modifier: Modifier = Modifier) {
        Row(modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { action ->
                val content = if (action.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                Surface(onClick = action.onClick, enabled = action.enabled,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = content,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant
                        .copy(alpha = if (action.enabled) 1f else 0.38f))) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                        Icon(action.icon, null, Modifier.size(21.dp))
                        Text(action.label, color = content, style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (action.configured) Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary) {
                            Icon(Icons.Default.Check, "${action.label} configured", Modifier.padding(2.dp))
                        }
                    }
                }
            }
        }
}
