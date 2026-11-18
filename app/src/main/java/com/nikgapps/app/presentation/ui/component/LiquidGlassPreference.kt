package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem

internal const val LIQUID_GLASS_DESCRIPTION =
    "Show a colorful background with glass project cards, header and navigation. " +
    "Turn off to use the standard background and solid cards. " +
    "Your light or dark theme still applies."

@Composable
internal fun LiquidGlassPreference() {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val enabled = DisplayPrefs.useLiquidGlass
    PreferenceItem(
        modifier = Modifier.toggleable(value = enabled, role = Role.Switch,
            onValueChange = { DisplayPrefs.useLiquidGlass = it }),
        leadingContent = { Icon(Icons.Outlined.BlurOn, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liquid glass", modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { showInfo = true }) {
                    Icon(AppIcons.Info, "About liquid glass", Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) }
    )
    if (showInfo) Dialog(onDismissRequest = { showInfo = false }) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Liquid glass", style = MaterialTheme.typography.titleLarge)
                    Text(LIQUID_GLASS_DESCRIPTION, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(onClick = { showInfo = false }, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp)) {
                    Text("Got it")
                }
            }
        }
    }
}
