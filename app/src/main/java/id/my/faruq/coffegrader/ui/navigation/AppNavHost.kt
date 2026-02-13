package id.my.faruq.coffegrader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import id.my.faruq.coffegrader.ui.about.AboutScreen
import id.my.faruq.coffegrader.ui.camera.CameraScreen
import id.my.faruq.coffegrader.ui.history.HistoryScreen
import id.my.faruq.coffegrader.ui.history.HistoryDetailScreen
import id.my.faruq.coffegrader.ui.history.HistoryDetailViewModel
import id.my.faruq.coffegrader.ui.history.HistoryViewModel
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

        // ================= HOME =================
        composable(Routes.HOME) {
            HomeScreen(
                onGoToCamera = { navController.navigate(Routes.CAMERA) },
                onGoToHistory = { navController.navigate(Routes.HISTORY) },
                onGoToAbout = { navController.navigate(Routes.ABOUT) },
                onRefreshHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onOpenHistoryDetail = { scanId ->
                    navController.navigate("${Routes.HISTORY_DETAIL}/$scanId")
                }
            )
        }

        // ================= CAMERA =================
        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // ================= HISTORY LIST =================
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
                onGoAbout = { navController.navigate(Routes.ABOUT) },
            )
        }

        // ================= ABOUT =================
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        // ================= HISTORY DETAIL =================
        composable(
            route = "${Routes.HISTORY_DETAIL}/{scanId}"
        ) { backStackEntry ->

            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""

            val detailVm: HistoryDetailViewModel = hiltViewModel(backStackEntry)

            HistoryDetailScreen(
                scanId = scanId,
                vm = detailVm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
