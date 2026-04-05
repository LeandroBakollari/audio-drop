package com.audiodrop.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Player : Screen("player")
    data object Library : Screen("library")
}
