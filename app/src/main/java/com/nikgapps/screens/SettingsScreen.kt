// SettingsScreen.kt
package com.nikgapps.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.data.SettingItem
import com.nikgapps.data.SettingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SharedViewModel) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(settings) { setting ->
                    SettingItemView(setting) { updatedSetting ->
                        viewModel.updateSetting(updatedSetting)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

@Composable
fun SettingItemView(setting: SettingItem, onSettingChanged: (SettingItem) -> Unit) {
    when (setting.type) {
        is SettingType.Toggle -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = setting.title)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = setting.value as Boolean,
                    onCheckedChange = { isChecked ->
                        onSettingChanged(setting.copy(value = isChecked))
                    }
                )
            }
        }
        is SettingType.Text -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = setting.title)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = setting.value as String,
                    onValueChange = { value ->
                        onSettingChanged(setting.copy(value = value))
                    }
                )
            }
        }
        // Add more types like Radio and Checkbox as needed
        SettingType.Checkbox -> TODO()
        SettingType.Radio -> TODO()
    }
}
