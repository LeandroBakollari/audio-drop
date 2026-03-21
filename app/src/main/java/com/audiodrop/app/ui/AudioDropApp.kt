package com.audiodrop.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.audiodrop.app.data.SampleData
import com.audiodrop.app.navigation.Screen
import com.audiodrop.app.ui.screens.HomeScreen
import com.audiodrop.app.ui.screens.LibraryScreen
import com.audiodrop.app.ui.screens.PlayerScreen

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun AudioDropApp() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Outlined.Home),
        BottomNavItem("Library", Screen.Library.route, Icons.Outlined.LibraryMusic)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route != null &&
                !currentDestination.route.orEmpty().startsWith("player/")

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    contentPadding = innerPadding,
                    recentItems = SampleData.recentItems,
                    onOpenPlayer = { itemId ->
                        navController.navigate(Screen.Player.createRoute(itemId))
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    contentPadding = innerPadding,
                    folders = SampleData.folders,
                    items = SampleData.recentItems,
                    onOpenItem = { itemId ->
                        navController.navigate(Screen.Player.createRoute(itemId))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("itemId") { defaultValue = "" })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
                val selectedItem = SampleData.recentItems.firstOrNull { it.id == itemId }
                    ?: SampleData.recentItems.first()

                PlayerScreen(
                    item = selectedItem,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
