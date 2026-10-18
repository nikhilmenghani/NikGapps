package com.nikgapps

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavBackStackEntry
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.utils.extensions.navigateWithState
import org.junit.Assert.*
import org.junit.Test

/** Real NavController, ephemeral graph; no activities, projects or app preferences are changed. */
class NavigationBackStackTest {
    @Navigator.Name("fixture")
    class FixtureNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)
        override fun navigate(entries: List<NavBackStackEntry>, navOptions: NavOptions?, navigatorExtras: Extras?) {
            entries.forEach { state.push(it) }
        }
        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) { state.pop(popUpTo, savedState) }
    }

    private fun controller(): NavHostController {
        val nav = NavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
        val fixture = FixtureNavigator()
        nav.navigatorProvider.addNavigator(fixture)
        nav.graph = NavGraph(NavGraphNavigator(nav.navigatorProvider)).apply {
            route = "root"
            listOf("Home", "Project", "Package", "Logs", "Settings").forEach { route ->
                addDestination(fixture.createDestination().apply { this.route = route })
            }
            setStartDestination("Home")
        }
        return nav
    }

    @Test fun returningHomeNeverRestoresProjectHistory() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val nav = controller()
            repeat(3) {
                nav.navigate("Project")
                nav.navigate("Package")
                nav.navigateWithState("Logs")
                nav.navigateWithState("Home")
                assertEquals("Home", nav.currentDestination?.route)
                assertNull(nav.previousBackStackEntry)
            }
            assertFalse(nav.popBackStack())
        }
    }

    @Test fun repeatedDetailsAndControllerRestoreHaveOneHomeRoot() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val nav = controller()
            nav.navigate("Project") { launchSingleTop = true }
            repeat(4) { nav.navigate("Package") { launchSingleTop = true } }
            assertTrue(nav.popBackStack())
            assertEquals("Project", nav.currentDestination?.route)
            val saved = nav.saveState()
            val restored = NavHostController(InstrumentationRegistry.getInstrumentation().targetContext)
            restored.navigatorProvider.addNavigator(FixtureNavigator())
            restored.restoreState(saved)
            restored.graph = nav.graph
            assertTrue(restored.popBackStack())
            assertEquals("Home", restored.currentDestination?.route)
            assertNull(restored.previousBackStackEntry)
            restored.navigateWithState("Home")
            assertEquals("Home", restored.currentDestination?.route)
        }
    }
}
