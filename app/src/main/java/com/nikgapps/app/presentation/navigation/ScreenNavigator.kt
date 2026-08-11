package com.nikgapps.app.presentation.navigation

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nikgapps.app.presentation.ui.screen.AppsScreen
import com.nikgapps.app.presentation.ui.screen.AppConfigScreen
import com.nikgapps.app.presentation.ui.screen.BuildZipScreen
import com.nikgapps.app.presentation.ui.screen.HomeScreen
import com.nikgapps.app.presentation.ui.screen.LogsScreen
import com.nikgapps.app.presentation.ui.screen.ProfileScreen
import com.nikgapps.app.presentation.ui.screen.ProjectScreen
import com.nikgapps.app.presentation.ui.screen.SettingsScreen
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.network.InternetRequiredGate
import com.nikgapps.app.update.MandatoryUpdateGate


data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screens.Home.name),
    NavItem("Logs", Icons.Default.Terminal, Screens.Logs.name)
)

enum class Screens {
    Home, Profile, Download, Settings, Apps, Logs, Install, Project
}

const val PROJECT_ROUTE = "Project/{projectId}?build={build}"
const val APP_CONFIG_ROUTE = "Project/{projectId}/app/{packageId}"
const val BUILD_ZIP_ROUTE = "Project/{projectId}/build"

fun projectRoute(projectId: String, build: Boolean = false) = "Project/$projectId?build=$build"
fun appConfigRoute(projectId: String, packageId: String) = "Project/$projectId/app/$packageId"
fun buildZipRoute(projectId: String) = "Project/$projectId/build"

val excludedScreens = listOf(
    Screens.Settings.name,
    Screens.Profile.name,
    Screens.Apps.name,
    PROJECT_ROUTE,
    APP_CONFIG_ROUTE,
    BUILD_ZIP_ROUTE
)

@Composable
fun ScreenNavigator(
    progressLogViewModel: ProgressLogViewModel
) {
    val navController: NavHostController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    InternetRequiredGate(
        required = globalClass.preferencesManager.displayPrefs.requireInternetAccess,
        allowOffline = currentEntry?.destination?.route == Screens.Settings.name,
        onOpenAppSettings = { navController.navigateWithState(Screens.Settings.name) }
    ) {
        MandatoryUpdateGate(
            enabled = globalClass.preferencesManager.displayPrefs.enforceAppUpdates,
            allowSettings = currentEntry?.destination?.route == Screens.Settings.name,
            onOpenSettings = { navController.navigateWithState(Screens.Settings.name) }
        ) {
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
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    if (currentDestination?.route !in excludedScreens) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)
                .animateContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOfNavItems.forEach { navItem ->
                    val selected = currentDestination?.hierarchy?.any { it.route == navItem.route } == true
                    ExpressiveNavigationItem(navItem, selected, {
                        if (!selected) navController.navigateWithState(route = navItem.route)
                    }, if (selected) Modifier.weight(1f) else Modifier.width(56.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpressiveNavigationItem(item: NavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "navigationContainer")
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navigationContent")
    Surface(onClick = onClick, modifier = modifier.semantics { role = Role.Tab; this.selected = selected },
        shape = RoundedCornerShape(20.dp), color = containerColor, contentColor = contentColor,
        tonalElevation = if (selected) 2.dp else 0.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = if (selected) null else item.label, Modifier.size(22.dp))
            AnimatedVisibility(selected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                Text(item.label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

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
                navController = navController
            )
        }
        composable(route = Screens.Profile.name) {
            ProfileScreen()
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
        composable(route = PROJECT_ROUTE, arguments = listOf(
            navArgument("build") { type = NavType.BoolType; defaultValue = false }
        )) { backStackEntry ->
            ProjectScreen(
                projectId = backStackEntry.arguments?.getString("projectId").orEmpty(),
                autoBuild = backStackEntry.arguments?.getBoolean("build") == true,
                navController = navController
            )
        }
        composable(route = APP_CONFIG_ROUTE) { backStackEntry ->
            AppConfigScreen(
                projectId = backStackEntry.arguments?.getString("projectId").orEmpty(),
                packageId = backStackEntry.arguments?.getString("packageId").orEmpty(),
                navController = navController
            )
        }
        composable(route = BUILD_ZIP_ROUTE) { backStackEntry ->
            BuildZipScreen(
                projectId = backStackEntry.arguments?.getString("projectId").orEmpty(),
                navController = navController
            )
        }
    }
}
