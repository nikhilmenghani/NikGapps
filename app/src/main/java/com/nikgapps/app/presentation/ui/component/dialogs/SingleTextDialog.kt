package com.nikgapps.app.presentation.ui.component.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.data.SingleText
import com.nikgapps.app.presentation.ui.component.bottomsheets.CustomBottomSheet

@Composable
fun SingleTextDialog() {
    val dialog = globalClass.singleTextDialog
    if (dialog.show) {
        CustomBottomSheet(
            title = dialog.title,
            sheetContent = { SingleText(dialog) },
            onDismiss = dialog::dismiss
        )
    }

}

@Composable
fun SingleText(dialog: SingleText) {
    var textState by remember { mutableStateOf(dialog.text) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            label = { Text(dialog.description) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                dialog.onConfirm(textState)
                dialog.dismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Confirm")
        }
    }
}
