package com.nikgapps.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nikgapps.app.presentation.ui.screen.AppsScreen
import com.nikgapps.app.presentation.ui.screen.DownloadScreen
import com.nikgapps.app.presentation.ui.screen.HomeScreen
import com.nikgapps.app.presentation.ui.screen.ProfileScreen
import com.nikgapps.app.presentation.ui.screen.SettingsScreen
import com.nikgapps.app.utils.extensions.navigateWithState

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screens.HomeScreen.name),
    NavItem("Profile", Icons.Default.Person, Screens.ProfileScreen.name),
    NavItem("Download", Icons.Default.KeyboardArrowDown, Screens.DownloadScreen.name),
    NavItem("Apps", Icons.Default.Apps, Screens.AppsScreen.name)
)

enum class Screens {
    HomeScreen, ProfileScreen, DownloadScreen, SettingsScreen, AppsScreen
}

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
