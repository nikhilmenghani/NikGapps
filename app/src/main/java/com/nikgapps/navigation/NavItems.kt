package com.nikgapps.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val listOfNavItems = listOf(
    NavItem("Home", Icons.Default.Home, Screens.HomeScreen.name),
    NavItem("Profile", Icons.Default.Person, Screens.ProfileScreen.name),
    NavItem("Settings", Icons.Default.Settings, Screens.SettingsScreen.name)
)