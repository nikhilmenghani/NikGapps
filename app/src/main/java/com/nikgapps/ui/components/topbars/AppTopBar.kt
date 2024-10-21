package com.nikgapps.ui.components.topbars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(
        title = { Text(text = title) },
        modifier = Modifier.statusBarsPadding()
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAppTopBar() {
    AppTopBar(title = "Installed Apps")
}