package com.nikgapps.app.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChangelogDialog(
    title: String,
    entries: List<ChangelogEntry>,
    loading: Boolean = false,
    onDismiss: () -> Unit,
    onUpdate: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    loading -> Text("Loading changelog…")
                    entries.isEmpty() -> Text("No changelog entries are available.")
                    else -> entries.forEachIndexed { index, entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Version ${entry.version}", style = MaterialTheme.typography.titleMedium)
                            entry.changes.forEach { change ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text("•", modifier = Modifier.padding(end = 8.dp))
                                    Text(change, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (index != entries.lastIndex) HorizontalDivider()
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (onUpdate == null) "Close" else "Later") } },
        confirmButton = {
            onUpdate?.let { action ->
                TextButton(onClick = action) { Text("Update now") }
            }
        }
    )
}
