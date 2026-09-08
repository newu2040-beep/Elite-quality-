package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.EliteQualityTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val currentTheme by viewModel.settingsRepo.theme.collectAsState()
            val darkModeSetting by viewModel.settingsRepo.darkMode.collectAsState()
            val compactModeSetting by viewModel.settingsRepo.compactMode.collectAsState()
            val compactConfig = com.example.ui.components.rememberCompactUiConfig(compactModeSetting)

            EliteQualityTheme(
                themeName = currentTheme,
                darkModeSetting = darkModeSetting
            ) {
                CompositionLocalProvider(
                    com.example.ui.components.LocalCompactUiConfig provides compactConfig
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { navController.navigate(Screen.Editor.route) },
                                onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                                onNavigateToPresets = { navController.navigate(Screen.Presets.route) },
                                onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                            )
                        }

                        composable(Screen.Editor.route) {
                            EditorScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToProcessing = { navController.navigate(Screen.Processing.route) },
                                onNavigateToExport = {
                                    navController.navigate(Screen.Export.route) {
                                        popUpTo(Screen.Editor.route) { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable(Screen.Processing.route) {
                            ProcessingScreen(
                                viewModel = viewModel,
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Export.route) {
                            ExportScreen(
                                viewModel = viewModel,
                                onNavigateHome = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                },
                                onContinueEditing = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Projects.route) {
                            ProjectsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenProject = { navController.navigate(Screen.Editor.route) }
                            )
                        }

                        composable(Screen.Presets.route) {
                            PresetsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onApplyPresetAndEdit = { navController.navigate(Screen.Editor.route) }
                            )
                        }

                        composable(Screen.Storage.route) {
                            StorageScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToStorage = { navController.navigate(Screen.Storage.route) }
                            )
                        }
                    }
                }
            }
        }
    }
}
