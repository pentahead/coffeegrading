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
import id.my.faruq.coffegrader.ui.sample.SampleInputScreen
import id.my.faruq.coffegrader.ui.sample.SampleInputViewModel

object Routes {
    const val HOME = "home"
    const val SAMPLE_INPUT = "sample_input"
    const val CAMERA = "camera"
    const val CAMERA_WITH_SAMPLE = "camera/{sampleInfoId}"
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
                onGoToCamera = { navController.navigate(Routes.SAMPLE_INPUT) },
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

        // ================= INPUT SAMPEL (sebelum scan) =================
        composable(Routes.SAMPLE_INPUT) {
            val vm: SampleInputViewModel = hiltViewModel()
            SampleInputScreen(
                vm = vm,
                onNextToScan = { sampleId ->
                    navController.navigate("camera/$sampleId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ================= CAMERA (dengan sampleInfoId dari input sampel) =================
        composable(route = Routes.CAMERA_WITH_SAMPLE) { backStackEntry ->
            val sampleInfoIdStr = backStackEntry.arguments?.getString("sampleInfoId") ?: "0"
            val sampleInfoId = sampleInfoIdStr.toLongOrNull() ?: 0L
            CameraScreen(
                sampleInfoId = if (sampleInfoId > 0) sampleInfoId else null,
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onSaveAndShowDetail = { historyId ->
                    navController.navigate("${Routes.HISTORY_DETAIL}/$historyId") {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
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
                onGoScan = { navController.navigate(Routes.SAMPLE_INPUT) },
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
