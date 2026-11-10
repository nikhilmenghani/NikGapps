package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nikgapps.app.registry.RegistryAvailability
import com.nikgapps.app.registry.registryRetryTime

internal fun projectCreationUnavailableMessage(retryAt: Long) =
    "You can create a project once NikGapps servers are available. " +
        "Please try again after ${registryRetryTime(retryAt)}."

/** Check before navigating or evaluating the account's project limit. */
@Composable
internal fun ProjectCreationGate(onCreate: () -> Unit, content: @Composable (() -> Unit) -> Unit) {
    var blockedUntil by remember { mutableStateOf<Long?>(null) }
    content {
        val retryAt = RegistryAvailability.activeRetryAt()
        if (retryAt != null) blockedUntil = retryAt else onCreate()
    }
    blockedUntil?.let { ProjectCreationUnavailableDialog(it) { blockedUntil = null } }
}

@Composable
internal fun ProjectCreationUnavailableDialog(retryAt: Long, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Project creation is temporarily unavailable", style = MaterialTheme.typography.titleLarge)
                    Text(projectCreationUnavailableMessage(retryAt), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp)) {
                    Text("OK")
                }
            }
        }
    }
}
