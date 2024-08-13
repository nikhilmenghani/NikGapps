package com.nikgapps.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.MainActivity
import com.nikgapps.navigation.Screens
import com.nikgapps.navigation.navigateWithState
import com.nikgapps.ui.components.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current as MainActivity
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        SettingsDialog(title = "Settings Test",
            onDismiss = { showDialog = false })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "NikGapps") },
                actions = {
                    IconButton(onClick = {
                        // Restart the activity to apply the new theme
                        context.restartActivity()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        navController.navigateWithState(
                            route = Screens.SettingsScreen.name
                        )
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(onClick = { showDialog = true }) {
                            Text(text = "Open Settings Dialog")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { /* Handle Install/Update */ }) {
                        Text(text = "Install/Update")
                    }
                    Button(onClick = { /* Handle Uninstall */ }) {
                        Text(text = "Uninstall")
                    }
                }
            }
        }
    )
}