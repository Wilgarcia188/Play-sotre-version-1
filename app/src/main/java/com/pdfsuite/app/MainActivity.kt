package com.pdfsuite.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pdfsuite.app.ui.theme.PdfSuiteTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        val sharedPdf = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        setContent {
            PdfSuiteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PdfSuiteApp(initialPdfUri = sharedPdf)
                }
            }
        }
    }
}
