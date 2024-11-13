package com.nikgapps.app.presentation.ui.component.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChooseAccountDialog(
    accounts: List<String>,
    selectedAccount: String,
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(selectedAccount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Choose an Account") },
        text = {
            Column {
                accounts.forEach { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == account,
                            onClick = {
                                selectedOption = account
                                onAccountSelected(account)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = account)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

