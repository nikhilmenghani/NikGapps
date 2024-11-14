package com.nikgapps.app.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
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
import androidx.navigation.compose.*
import com.nikgapps.app.presentation.ui.screen.AppsScreen
import com.nikgapps.app.presentation.ui.screen.DownloadScreen
import com.nikgapps.app.presentation.ui.screen.HomeScreen
import com.nikgapps.app.presentation.ui.screen.ProfileScreen
import com.nikgapps.app.presentation.ui.screen.SettingsScreen
import com.nikgapps.app.presentation.ui.screen.PermissionsScreen
import com.nikgapps.app.utils.extensions.navigateWithState


data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screens.Home.name),
    NavItem("Profile", Icons.Default.Person, Screens.Profile.name),
    NavItem("Download", Icons.Default.Download, Screens.Download.name),
    NavItem("Apps", Icons.Default.Apps, Screens.Apps.name)
)

enum class Screens {
    Home, Profile, Download, Settings, Apps, Permissions
}

val excludedScreens = listOf(Screens.Settings.name, Screens.Permissions.name)

@Composable
fun ScreenNavigator() {
    val navController: NavHostController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    if (currentDestination?.route !in excludedScreens) {
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

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screens.Permissions.name,
        modifier = modifier
    ) {
        composable(route = Screens.Home.name) {
            HomeScreen(navController = navController)
        }
        composable(route = Screens.Profile.name) {
            ProfileScreen()
        }
        composable(route = Screens.Download.name) {
            DownloadScreen()
        }
        composable(route = Screens.Apps.name) {
            AppsScreen()
        }
        composable(route = Screens.Settings.name) {
            SettingsScreen(navController = navController)
        }
        composable(route = Screens.Permissions.name) {
            PermissionsScreen(navController = navController)
        }
    }
}
