package com.nikgapps.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

fun NavController.navigateWithState(
    route: String,
    inclusive: Boolean = false,
    launchSingleTop: Boolean = true,
    restoreState: Boolean = true
) {
    this.navigate(route) {
        popUpTo(this@navigateWithState.graph.findStartDestination().id) {
            this.saveState = restoreState
            // Set inclusive to true if we want to store the state on back stack
            // this will go reverse in navigation upon clicking back as opposed to jumping to Home screen
            this.inclusive = inclusive
        }
        this.launchSingleTop = launchSingleTop
        this.restoreState = restoreState
    }
}
