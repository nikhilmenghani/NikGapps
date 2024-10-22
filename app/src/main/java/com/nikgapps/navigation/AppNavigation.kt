package com.nikgapps.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nikgapps.ui.screens.apps.AppsScreen
import com.nikgapps.ui.screens.downloads.DownloadScreen
import com.nikgapps.ui.screens.home.HomeScreen
import com.nikgapps.ui.screens.profile.ProfileScreen
import com.nikgapps.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != Screens.SettingsScreen.name) {
                NavigationBar {
                    listOfNavItems.forEach { navItem: NavItem ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = navItem.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(text = navItem.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == navItem.route } == true,
                            onClick = {
                                if (currentDestination?.route != navItem.route) {
                                    navController.navigateWithState(
                                        route = navItem.route
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screens.HomeScreen.name,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route = Screens.HomeScreen.name) {
                HomeScreen(navController = navController)
            }
            composable(route = Screens.ProfileScreen.name) {
                ProfileScreen()
            }
            composable(route = Screens.DownloadScreen.name) {
                DownloadScreen()
            }
            composable(route = Screens.AppsScreen.name) {
                AppsScreen()
            }
            composable(route = Screens.SettingsScreen.name) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
