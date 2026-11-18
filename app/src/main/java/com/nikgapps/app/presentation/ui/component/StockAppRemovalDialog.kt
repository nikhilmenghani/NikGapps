package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.nikgapps.app.presentation.ui.component.GlassButton as Button
import com.nikgapps.app.presentation.ui.component.GlassTonalButton as FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Preview the installer's actual removal targets before changing the stock-app preference. */
@Composable
internal fun StockAppRemovalDialog(
    packageName: String,
    targets: List<String>,
    keep: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    selectOnConfirm: Boolean = false
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .85f
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                Column(Modifier.weight(1f, fill = false).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (keep) "Keep stock apps?" else "Remove stock apps?",
                        style = MaterialTheme.typography.titleLarge)
                    Text(if (keep && selectOnConfirm)
                        "Select $packageName for your build and keep these apps when flashing. They are otherwise configured for removal."
                    else if (selectOnConfirm)
                        "Select $packageName for your build and allow it to remove these apps when flashing."
                    else if (keep)
                        "Keep these apps when flashing $packageName. They are otherwise configured for removal."
                    else "Allow $packageName to remove these apps when flashing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GlassSurface(modifier = Modifier.weight(1f, fill = false),
                        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(targets) { target -> Text(target, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    Text("Only matching apps present in your ROM are affected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Cancel") }
                    Button(onClick = onConfirm, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(if (keep && selectOnConfirm) "Select & keep"
                            else if (selectOnConfirm) "Select & remove"
                            else if (keep) "Keep apps" else "Remove apps")
                    }
                }
            }
        }
    }
}
