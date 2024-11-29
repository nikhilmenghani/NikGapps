package com.nikgapps.app.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.nikgapps.app.presentation.ui.screen.InstallScreen
import com.nikgapps.app.presentation.ui.screen.LogsScreen
import com.nikgapps.app.presentation.ui.screen.ProfileScreen
import com.nikgapps.app.presentation.ui.screen.SettingsScreen
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.extensions.navigateWithState


data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screens.Home.name),
    NavItem("Download", Icons.Default.Download, Screens.Download.name),
    NavItem("Install", Icons.Default.InstallMobile, Screens.Install.name),
    NavItem("Logs", Icons.Default.Terminal, Screens.Logs.name)
)

enum class Screens {
    Home, Profile, Download, Settings, Apps, Logs, Install
}

val excludedScreens = listOf(Screens.Settings.name, Screens.Profile.name, Screens.Apps.name)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ScreenNavigator(
    progressLogViewModel: ProgressLogViewModel
) {
    val navController: NavHostController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
    ) { innerPadding ->
        NavigationHost(
            navController = navController,
            progressLogViewModel,
            modifier = Modifier.padding(innerPadding)
        )
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavigationHost(
    navController: NavHostController,
    progressLogViewModel: ProgressLogViewModel,
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screens.Home.name,
        modifier = modifier
    ) {
        composable(route = Screens.Home.name) {
            HomeScreen(
                navController = navController,
                progressLogViewModel = progressLogViewModel
            )
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
        composable(route = Screens.Logs.name) {
            LogsScreen()
        }
        composable(route = Screens.Install.name) {
            InstallScreen(progressLogViewModel)
        }
    }
}
