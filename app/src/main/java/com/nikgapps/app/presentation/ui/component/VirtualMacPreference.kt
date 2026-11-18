package com.nikgapps.app.presentation.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PhonelinkRing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nikgapps.app.data.VirtualDeviceIdentity
import com.nikgapps.app.presentation.ui.component.dialogs.AccountDialog
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem

@Composable
internal fun VirtualMacPreference() {
    val context = LocalContext.current
    val identity = remember(context) { VirtualDeviceIdentity.read(context) }
    var info by rememberSaveable { mutableStateOf(false) }
    PreferenceItem(
        leadingContent = { Icon(Icons.Outlined.PhonelinkRing, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Virtual MAC", style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { info = true }) {
                    Icon(AppIcons.Info, "About virtual MAC", Modifier.size(20.dp))
                }
            }
        },
        supportingText = { Text(identity?.virtualMac ?: "Unavailable on this device",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            IconButton(enabled = identity != null, onClick = {
                val mac = identity?.virtualMac ?: return@IconButton
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("NikGapps virtual MAC", mac))
                Toast.makeText(context, "Virtual MAC copied", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Outlined.ContentCopy, "Copy virtual MAC", Modifier.size(20.dp)) }
        }
    )
    if (info) AccountDialog(title = "Virtual MAC", onDismiss = { info = false },
        content = { Text(
            "This virtual MAC address is generated to uniquely identify your device across multiple app reinstalls.",
            style = MaterialTheme.typography.bodyMedium) },
        actions = { GlassButton(onClick = { info = false }, modifier = Modifier.fillMaxWidth()) { Text("Got it") } })
}
