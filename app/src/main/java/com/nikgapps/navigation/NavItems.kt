package com.nikgapps.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

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