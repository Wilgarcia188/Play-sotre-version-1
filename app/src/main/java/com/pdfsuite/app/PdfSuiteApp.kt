package com.pdfsuite.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pdfsuite.app.ui.home.HomeScreen
import com.pdfsuite.app.ui.merge.MergeScreen
import com.pdfsuite.app.ui.split.SplitScreen
import com.pdfsuite.app.ui.viewer.ViewerScreen

object Routes {
    const val HOME = "home"
    const val VIEWER = "viewer"
    const val MERGE = "merge"
    const val SPLIT = "split"
}

@Composable
fun PdfSuiteApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenViewer = { navController.navigate(Routes.VIEWER) },
                onOpenMerge = { navController.navigate(Routes.MERGE) },
                onOpenSplit = { navController.navigate(Routes.SPLIT) },
            )
        }
        composable(Routes.VIEWER) {
            ViewerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MERGE) {
            MergeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SPLIT) {
            SplitScreen(onBack = { navController.popBackStack() })
        }
    }
}
