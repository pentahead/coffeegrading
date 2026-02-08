package id.my.faruq.coffegrader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.my.faruq.coffegrader.ui.about.AboutScreen
import id.my.faruq.coffegrader.ui.camera.CameraScreen
import id.my.faruq.coffegrader.ui.history.HistoryScreen
import id.my.faruq.coffegrader.ui.history.HistoryDetailScreen
import id.my.faruq.coffegrader.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val HISTORY = "history"
    const val ABOUT = "about"
    const val HISTORY_DETAIL = "history_detail"

}


@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onGoToCamera = { navController.navigate(Routes.CAMERA) },
                onGoToHistory = { navController.navigate(Routes.HISTORY) },
                onGoToAbout = { navController.navigate(Routes.ABOUT) },
                onRefreshHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CAMERA) { CameraScreen(
            onNavigateToHome = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }
        )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenDetail = { scanId ->
                    navController.navigate("${Routes.HISTORY_DETAIL}/$scanId")
                },
                onGoHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onGoScan = { navController.navigate(Routes.CAMERA) },
                onGoHistoryRefresh = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.HISTORY) { inclusive = true }
                    }
                },
                onGoAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        composable( "${Routes.HISTORY_DETAIL}/{scanId}") { backStackEntry ->
            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
            HistoryDetailScreen(
                scanId = scanId,
                onBack = {navController.popBackStack()}
            )
        }
        }
    }

