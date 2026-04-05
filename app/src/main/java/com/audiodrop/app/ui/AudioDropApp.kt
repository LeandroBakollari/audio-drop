package com.audiodrop.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audiodrop.app.model.MediaItemUi
import com.audiodrop.app.model.progressFraction
import com.audiodrop.app.navigation.Screen
import com.audiodrop.app.ui.components.AudioArtwork
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
    val state = rememberAudioDropState()
    val bottomNavItems = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Outlined.Home),
        BottomNavItem("Library", Screen.Library.route, Icons.Outlined.LibraryMusic)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route != Screen.Player.route

            if (showBottomBar) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.currentItem?.let { item ->
                        MiniPlayerBar(
                            item = item,
                            subtitle = state.itemSubtitle(item),
                            isPlaying = state.isPlaying,
                            onTogglePlayPause = state::togglePlayPause,
                            onOpenPlayer = {
                                navController.navigate(Screen.Player.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    contentPadding = innerPadding,
                    state = state,
                    onOpenPlayer = { itemId ->
                        state.openItem(itemId)
                        navController.navigate(Screen.Player.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenLibrary = { folderId ->
                        state.setActiveFolder(folderId)
                        navController.navigate(Screen.Library.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    contentPadding = innerPadding,
                    state = state,
                    onOpenItem = { itemId ->
                        state.openItem(itemId)
                        navController.navigate(Screen.Player.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Player.route) {
                PlayerScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    item: MediaItemUi,
    subtitle: String,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onOpenPlayer),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudioArtwork(
                    item = item,
                    modifier = Modifier.size(52.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Outlined.PauseCircle
                        } else {
                            Icons.Outlined.PlayCircle
                        },
                        contentDescription = "Toggle playback",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { item.progressFraction },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
