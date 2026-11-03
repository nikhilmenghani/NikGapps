package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
internal fun DebloaterReviewTargets(targets: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button,
                onClickLabel = if (expanded) "Hide debloater targets" else "Show debloater targets",
                onClick = { expanded = !expanded }).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                }
                Column(Modifier.weight(1f)) {
                    Text("Debloater", style = MaterialTheme.typography.titleSmall)
                    Text("${targets.size} targets · Removed when flashing", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Hide targets" else "Show targets")
            }
        }
        AppSetContents(expanded) {
            targets.forEachIndexed { index, target ->
                Text(target, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                if (index < targets.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            }
        }
    }
}
