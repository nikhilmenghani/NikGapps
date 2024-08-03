package com.nikgapps.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.nikgapps.navigation.Screens
import com.nikgapps.navigation.navigateWithState
import com.nikgapps.ui.theme.NikGappsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "NikGapps") },
                actions = {
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
                Text(text = "Installed Version: v23")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Manager Version: v23.x")
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "SafetyNet Check")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {}) {
                            Text(text = "Run SafetyNet Check")
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

@Preview(
    name = "Home Screen - Light Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun HomeScreenLightPreview() {
    NikGappsTheme(darkTheme = false) {
        val navController = rememberNavController()
        HomeScreen(navController)
    }
}

@Preview(
    name = "Home Screen - Dark Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenDarkPreview() {
    NikGappsTheme(darkTheme = true) {
        val navController = rememberNavController()
        HomeScreen(navController)
    }
}
