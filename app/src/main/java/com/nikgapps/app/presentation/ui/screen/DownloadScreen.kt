package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.ui.component.cards.DownloadNikGappsCard
import com.nikgapps.app.presentation.ui.component.dialogs.SingleChoiceDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download") }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp), // Apply padding from Scaffold to avoid overlap
            ) {
                SingleChoiceDialog()
                DownloadNikGappsCard()
            }
        }
    )
}
