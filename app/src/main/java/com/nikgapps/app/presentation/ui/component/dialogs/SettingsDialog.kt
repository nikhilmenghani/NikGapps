package com.nikgapps.app.presentation.ui.component.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing here to prevent dismissal on outside click */ },
            title = { Text(text = title) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        // settings item to display
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    onDismiss()
                }) {
                    Text(text = "OK")
                }
            }
        )
    }
}

@Preview
@Composable
fun PreviewSettingsDialog() {
    SettingsDialog("Test", onDismiss = {})
}
