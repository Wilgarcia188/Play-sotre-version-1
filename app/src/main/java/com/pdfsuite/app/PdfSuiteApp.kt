package com.pdfsuite.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfsuite.app.ui.annotate.AnnotateScreen
import com.pdfsuite.app.ui.compress.CompressScreen
import com.pdfsuite.app.ui.convert.ConvertScreen
import com.pdfsuite.app.ui.home.HomeScreen
import com.pdfsuite.app.ui.merge.MergeScreen
import com.pdfsuite.app.ui.ocr.OcrScreen
import com.pdfsuite.app.ui.organize.OrganizeScreen
import com.pdfsuite.app.ui.scan.ScanScreen
import com.pdfsuite.app.ui.split.SplitScreen
import com.pdfsuite.app.ui.viewer.ViewerScreen
import com.pdfsuite.app.ui.watermark.WatermarkScreen

object Routes {
    const val HOME = "home"
    const val VIEWER = "viewer"
    const val MERGE = "merge"
    const val SPLIT = "split"
    const val OCR = "ocr"
    const val COMPRESS = "compress"
    const val CONVERT = "convert"
    const val WATERMARK = "watermark"
    const val SCAN = "scan"
    const val ANNOTATE = "annotate"
    const val ORGANIZE = "organize"

    const val VIEWER_URI_ARG = "uri"
    const val VIEWER_WITH_ARG = "$VIEWER?$VIEWER_URI_ARG={$VIEWER_URI_ARG}"

    fun viewerFor(uri: Uri): String = "$VIEWER?$VIEWER_URI_ARG=${Uri.encode(uri.toString())}"
}

@Composable
fun PdfSuiteApp(initialPdfUri: Uri? = null) {
    val navController = rememberNavController()
    val openInViewer: (Uri) -> Unit = { uri -> navController.navigate(Routes.viewerFor(uri)) }

    LaunchedEffect(initialPdfUri) {
        initialPdfUri?.let { navController.navigate(Routes.viewerFor(it)) }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenViewer = { navController.navigate(Routes.VIEWER) },
                onOpenMerge = { navController.navigate(Routes.MERGE) },
                onOpenSplit = { navController.navigate(Routes.SPLIT) },
                onOpenOcr = { navController.navigate(Routes.OCR) },
                onOpenCompress = { navController.navigate(Routes.COMPRESS) },
                onOpenConvert = { navController.navigate(Routes.CONVERT) },
                onOpenWatermark = { navController.navigate(Routes.WATERMARK) },
                onOpenScan = { navController.navigate(Routes.SCAN) },
                onOpenAnnotate = { navController.navigate(Routes.ANNOTATE) },
                onOpenOrganize = { navController.navigate(Routes.ORGANIZE) },
            )
        }
        composable(
            route = Routes.VIEWER_WITH_ARG,
            arguments = listOf(
                navArgument(Routes.VIEWER_URI_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString(Routes.VIEWER_URI_ARG)?.let(Uri::parse)
            ViewerScreen(onBack = { navController.popBackStack() }, initialUri = uri)
        }
        composable(Routes.MERGE) {
            MergeScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.SPLIT) {
            SplitScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.OCR) {
            OcrScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.CONVERT) {
            ConvertScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.WATERMARK) {
            WatermarkScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.SCAN) {
            ScanScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.ANNOTATE) {
            AnnotateScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
        composable(Routes.ORGANIZE) {
            OrganizeScreen(onBack = { navController.popBackStack() }, onOpenInViewer = openInViewer)
        }
    }
}
