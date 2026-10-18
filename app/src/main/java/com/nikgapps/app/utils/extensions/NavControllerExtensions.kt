package com.nikgapps.app.utils.extensions

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

fun NavController.navigateWithState(
    route: String,
    inclusive: Boolean = false,
    launchSingleTop: Boolean = true,
    restoreState: Boolean = true
) {
    val home = this.graph.findStartDestination()
    if (route == home.route) {
        // Home is the existing root, not a tab whose saved child stack should
        // be restored. Restoring it can resurrect projects/package screens.
        if (currentDestination?.route == route) {
            popBackStack(home.id, inclusive = false)
            return
        }
        if (popBackStack(home.id, inclusive = false)) return
        navigate(route) {
            popUpTo(graph.id) { this.inclusive = false }
            this.launchSingleTop = true
            this.restoreState = false
        }
        return
    }
    this.navigate(route) {
        popUpTo(home.id) {
            this.saveState = restoreState
            this.inclusive = inclusive
        }
        this.launchSingleTop = launchSingleTop
        this.restoreState = restoreState
    }
}
