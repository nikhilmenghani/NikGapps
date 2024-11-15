package com.nikgapps.app.presentation.ui.screen

import ModalBottomSheetM3Example
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
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.app.presentation.ui.component.buttons.CopyFileButton
import com.nikgapps.app.presentation.ui.component.buttons.DownloadAndExtractButton
import com.nikgapps.app.presentation.ui.component.buttons.DownloadButton
import com.nikgapps.app.presentation.ui.component.buttons.RequestNotificationPermission
import com.nikgapps.app.presentation.ui.component.cards.GetRootAccessCard
import com.nikgapps.app.presentation.ui.component.cards.UpdateAppCard
import com.nikgapps.app.presentation.ui.component.common.RootStatusDisplay
import com.nikgapps.app.presentation.ui.component.dialogs.SettingsDialog
import com.nikgapps.MainActivity

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
                            route = Screens.Settings.name
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
                RootStatusDisplay()
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(onClick = { showDialog = true }) {
                            Text(text = "Open Settings Dialog")
                        }
                        CopyFileButton()
                        DownloadAndExtractButton()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                GetRootAccessCard()
                UpdateAppCard()
                DownloadButton(context)
            }
        }
    )
}