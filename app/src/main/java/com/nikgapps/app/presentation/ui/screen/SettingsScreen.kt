// SettingsScreen.kt
package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.utils.extensions.Space
import com.nikgapps.app.presentation.ui.component.containers.DisplayContainer
import com.nikgapps.app.presentation.ui.component.dialogs.SingleChoiceDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            SingleChoiceDialog()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                Space(size = 4.dp)

                DisplayContainer()

                Space(size = 4.dp)
            }
        }
    )
}
