package com.nikgapps.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Red // Setting the TopAppBar color to Red
                )
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue) // Set the background color to White
                    .padding(paddingValues), // Apply padding from Scaffold to avoid overlap
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Download Screen",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
    )
}
