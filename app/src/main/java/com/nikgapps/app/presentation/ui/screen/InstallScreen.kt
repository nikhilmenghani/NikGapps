package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.ui.component.cards.ExecuteMountCard
import com.nikgapps.app.presentation.ui.component.cards.InstallZipCard
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(progressLogViewModel: ProgressLogViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install NikGapps") }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item { InstallZipCard(progressLogViewModel) }
                item { ExecuteMountCard() }
            }
        }
    )
}