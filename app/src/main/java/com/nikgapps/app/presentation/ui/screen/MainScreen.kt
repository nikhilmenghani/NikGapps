package com.nikgapps.app.presentation.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.nikgapps.app.presentation.navigation.NavItem
import com.nikgapps.app.presentation.navigation.Screens
import com.nikgapps.app.presentation.navigation.listOfNavItems
import com.nikgapps.app.utils.extensions.navigateWithState

@Composable
fun MainScreen() {
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

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screens.HomeScreen.name,
        modifier = modifier
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