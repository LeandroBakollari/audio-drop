package com.audiodrop.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Player : Screen("player/{itemId}") {
        fun createRoute(itemId: String): String = "player/$itemId"
    }
    data object Library : Screen("library")
}
