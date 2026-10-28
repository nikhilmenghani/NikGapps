package com.nikgapps.app.presentation.navigation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.nikgapps.app.presentation.ui.screen.PullRequestsScreen
import com.nikgapps.app.presentation.ui.screen.ProjectScreen
import com.nikgapps.app.presentation.ui.screen.SettingsScreen
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.extensions.navigateWithState
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.network.InternetRequiredGate
import com.nikgapps.app.update.MandatoryUpdateGate
import com.nikgapps.app.utils.AppDiagnostics
import com.nikgapps.app.analytics.AppAnalytics
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import com.nikgapps.app.presentation.ui.screen.AccountGate


data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = buildList {
    add(NavItem("Home", Icons.Default.Home, Screens.Home.name))
}

enum class Screens {
    Home, Profile, Download, Settings, Apps, Requests, Logs, Install, Project
}

const val PROJECT_ROUTE = "Project/{projectId}?build={build}"
const val CREATE_PROJECT_REQUEST = "create_project_requested"
const val APP_CONFIG_ROUTE = "Project/{projectId}/app/{packageId}"
const val BUILD_ZIP_ROUTE = "Project/{projectId}/build?operation={operation}"
const val BUILD_LATEST_ZIP_ROUTE = "Project/{projectId}/build-latest?operation={operation}"

fun projectRoute(projectId: String, build: Boolean = false) = "Project/$projectId?build=$build"
fun appConfigRoute(projectId: String, packageId: String) = "Project/$projectId/app/$packageId"
fun buildZipRoute(projectId: String, latest: Boolean = false): String {
    val operation = com.nikgapps.app.utils.worker.BuildWorkScheduler.currentOperation(com.nikgapps.App.appContext, projectId)
        ?: java.util.UUID.randomUUID().toString()
    return "Project/$projectId/${if (latest) "build-latest" else "build"}?operation=$operation"
}

@Composable
fun ScreenNavigator(
    progressLogViewModel: ProgressLogViewModel
) {
    var showSignInSettings by rememberSaveable { mutableStateOf(false) }
    val token = GithubPrefs.token
    val username = GithubPrefs.username
    val signedIn = token.isNotBlank() && username.isNotBlank()
    LaunchedEffect(token, username) {
        if (token.isNotBlank()) {
            try {
                val profile = if (username.isBlank()) {
                    GitHubDeviceAuth.accountProfileWithRetry(token)
                } else {
                    GitHubDeviceAuth.accountProfile(token)
                }
                if (GithubPrefs.token == token && GithubPrefs.username.isBlank()) {
                    GithubPrefs.username = profile.login
                    GithubPrefs.lastUsername = profile.login
                    GithubPrefs.avatarUrl = profile.avatarUrl
                }
            } catch (_: GitHubDeviceAuth.InvalidTokenException) {
                if (GithubPrefs.token == token) {
                    if (GithubPrefs.username.isNotBlank()) GithubPrefs.lastUsername = GithubPrefs.username
                    GithubPrefs.token = ""
                    GithubPrefs.username = ""
                    GithubPrefs.avatarUrl = ""
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the saved session during network or GitHub service failures.
            }
        }
    }
    LaunchedEffect(signedIn) {
        if (signedIn) {
            showSignInSettings = false
            GithubPrefs.guestMode = false
        }
    }
    if (!signedIn && !(GithubPrefs.guestMode && DisplayPrefs.guestAccessEnabled)) {
        BackHandler(enabled = showSignInSettings) { showSignInSettings = false }
        if (showSignInSettings) {
            SettingsScreen(onBack = { showSignInSettings = false }, startOnAccount = true)
        } else {
            AccountGate(onOpenSettings = { showSignInSettings = true },
                onContinueAsGuest = { GithubPrefs.continueAsGuest() })
        }
        return
    }
    val navController: NavHostController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry) {
        currentEntry?.destination?.route?.let { route ->
            AppDiagnostics.info("navigation", "screen_opened", mapOf("screen" to route))
            val screen = when (route) {
                PROJECT_ROUTE -> "project"
                APP_CONFIG_ROUTE -> "app_config"
                BUILD_ZIP_ROUTE -> "build_zip"
                else -> Screens.entries.firstOrNull { it.name == route }?.name?.lowercase() ?: "other"
            }
            AppAnalytics.track("app_screen_opened", mapOf("screen" to screen))
        }
    }
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
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NavigationHost(
                    navController = navController,
                    progressLogViewModel,
                    modifier = Modifier.fillMaxSize().clipToBounds()
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, selectedRoute: String, onShowInfo: () -> Unit) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.fillMaxWidth().padding(4.dp)
                .animateContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOfNavItems.forEach { navItem ->
                    val selected = selectedRoute == navItem.route
                    ExpressiveNavigationItem(navItem, selected, {
                        if (!selected) navController.navigateWithState(route = navItem.route)
                    }, if (selected) Modifier.weight(1f) else Modifier.width(56.dp))
                }
                Surface(onClick = onShowInfo,
                    modifier = Modifier.width(56.dp).semantics { role = Role.Button },
                    shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, "Discover NikGapps features", Modifier.size(22.dp))
                    }
                }
            }
        }
}

/** Navigation and the screen action are independent siblings, not mixed nav items. */
@Composable
private fun ScreenBottomBar(onCreateProject: () -> Unit, navigation: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { navigation() }
            Surface(onClick = onCreateProject,
                modifier = Modifier.size(56.dp).semantics { role = Role.Button },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "Create project", Modifier.size(24.dp))
                }
            }
        }
    }
}

/** Keep each destination's insets stable while its entire screen transitions. */
@Composable
private fun NavigationTab(navController: NavHostController, route: String, content: @Composable () -> Unit) {
    var showFeatureGuide by rememberSaveable { mutableStateOf(false) }
    Scaffold(bottomBar = {
        ScreenBottomBar(onCreateProject = {
            if (route != Screens.Home.name) navController.navigateWithState(Screens.Home.name)
            navController.getBackStackEntry(Screens.Home.name).savedStateHandle[CREATE_PROJECT_REQUEST] = true
        }) { BottomNavigationBar(navController, route, onShowInfo = { showFeatureGuide = true }) }
    },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
    if (showFeatureGuide) com.nikgapps.app.presentation.ui.component.AppFeatureGuideDialog {
        showFeatureGuide = false
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
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(160, delayMillis = 70))
        },
        exitTransition = {
            fadeOut(tween(70))
        },
        popEnterTransition = {
            fadeIn(tween(160, delayMillis = 70))
        },
        popExitTransition = {
            fadeOut(tween(70))
        },
        predictivePopEnterTransition = {
            fadeIn(tween(120, delayMillis = 120, easing = LinearEasing))
        },
        predictivePopExitTransition = {
            fadeOut(tween(120, easing = LinearEasing))
        }
    ) {
        composable(route = Screens.Home.name) {
            val activity = LocalActivity.current
            val entry by navController.currentBackStackEntryAsState()
            // Home is terminal even if an older saved stack contained history
            // below it. Screen-local dialogs register later and consume Back first.
            BackHandler(enabled = activity != null && entry?.destination?.route == Screens.Home.name) { activity?.finish() }
            NavigationTab(navController, Screens.Home.name) { HomeScreen(navController = navController) }
        }
        composable(route = Screens.Profile.name) {
            ProfileScreen()
        }
        composable(route = Screens.Apps.name) {
            AppsScreen()
        }
        composable(route = Screens.Settings.name) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screens.Logs.name) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screens.Requests.name) {
            NavigationTab(navController, Screens.Requests.name) { PullRequestsScreen() }
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
        composable(route = BUILD_LATEST_ZIP_ROUTE) { backStackEntry ->
            BuildZipScreen(projectId = backStackEntry.arguments?.getString("projectId").orEmpty(),
                navController = navController, latestRelease = true,
                operationId = backStackEntry.arguments?.getString("operation").orEmpty())
        }
        composable(route = BUILD_ZIP_ROUTE) { backStackEntry ->
            BuildZipScreen(
                projectId = backStackEntry.arguments?.getString("projectId").orEmpty(),
                navController = navController,
                operationId = backStackEntry.arguments?.getString("operation").orEmpty()
            )
        }
    }
}
