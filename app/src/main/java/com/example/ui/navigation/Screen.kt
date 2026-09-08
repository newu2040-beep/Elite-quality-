package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor")
    object Processing : Screen("processing")
    object Export : Screen("export")
    object Projects : Screen("projects")
    object Presets : Screen("presets")
    object Storage : Screen("storage")
    object Settings : Screen("settings")
}
