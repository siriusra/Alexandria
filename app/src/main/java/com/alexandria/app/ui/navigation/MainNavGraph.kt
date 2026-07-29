package com.alexandria.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.components.BottomNavBar
import com.alexandria.app.ui.components.VignetteOverlay
import com.alexandria.app.ui.components.bookshelf.shelfGridBackground
import com.alexandria.app.ui.screens.addbook.AddBookScreen
import com.alexandria.app.ui.screens.detail.BookDetailScreen
import com.alexandria.app.ui.screens.home.HomeScreen
import com.alexandria.app.ui.screens.library.LibraryScreen
import com.alexandria.app.ui.screens.search.SearchScreen
import com.alexandria.app.ui.screens.settings.FuentesSettingsScreen
import com.alexandria.app.ui.screens.settings.SettingsScreen
import com.alexandria.app.ui.screens.update.UpdateScreen
import com.alexandria.app.ui.theme.LocalVisualMode

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val visualMode = LocalVisualMode.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (visualMode == VisualMode.IMMERSIVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shelfGridBackground(visualMode)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToAddBook = {
                            navController.navigate(Screen.AddBook.route)
                        },
                        onNavigateToBookDetail = { bookId ->
                            navController.navigate(Screen.BookDetail.createRoute(bookId))
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        onNavigateToBookDetail = { bookId ->
                            navController.navigate(Screen.BookDetail.createRoute(bookId))
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onNavigateToBookDetail = { bookId ->
                            navController.navigate(Screen.BookDetail.createRoute(bookId))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToUpdate = {
                            navController.navigate(Screen.Update.route)
                        },
                        onNavigateToFuentesSettings = {
                            navController.navigate(Screen.FuentesSettings.route)
                        }
                    )
                }

                composable(Screen.FuentesSettings.route) {
                    FuentesSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Update.route) {
                    UpdateScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.BookDetail.route,
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.LongType }
                    )
                ) {
                    BookDetailScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToEdit = { bookId ->
                            navController.navigate(Screen.EditBook.createRoute(bookId))
                        }
                    )
                }

                composable(Screen.AddBook.route) {
                    AddBookScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.EditBook.route,
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.LongType }
                    )
                ) {
                    AddBookScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }

        if (visualMode == VisualMode.IMMERSIVE) {
            VignetteOverlay(visualMode = visualMode)
        }
    }
}
