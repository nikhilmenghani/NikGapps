package com.nikgapps.app.presentation.ui.component.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialog(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onDismissRequest() },
        sheetState = sheetState
    ) {
        content()
    }
}