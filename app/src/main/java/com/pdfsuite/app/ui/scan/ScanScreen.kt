package com.pdfsuite.app.ui.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.pdfsuite.app.R
import com.pdfsuite.app.pdf.findActivity
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(onBack: () -> Unit, viewModel: ScanViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val successMessage = stringResource(R.string.scan_save_success)
    val errorMessage = stringResource(R.string.scan_save_error)
    val scanErrorMessage = stringResource(R.string.scan_start_error)

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pdf = scanningResult?.pdf
        viewModel.onScanResult(pdf?.uri, pdf?.pageCount ?: 0)
    }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.save(it) }
    }

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            ScanStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            ScanStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
    }

    fun startScan() {
        val currentActivity = activity ?: return
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(currentActivity)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                coroutineScope.launch { snackbarHostState.showSnackbar(scanErrorMessage) }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (uiState.status) {
                ScanStatus.IDLE -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.scan_empty))
                        Button(onClick = { startScan() }) {
                            Text(stringResource(R.string.scan_start))
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (uiState.status) {
                            ScanStatus.SAVING -> CircularProgressIndicator()
                            else -> Text(
                                stringResource(R.string.scan_pages_ready, uiState.pageCount),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }

                    Button(
                        onClick = { savePdf.launch("documento_escaneado.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.status != ScanStatus.SAVING,
                    ) {
                        Text(stringResource(R.string.scan_save))
                    }
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = uiState.status != ScanStatus.SAVING,
                    ) {
                        Text(stringResource(R.string.scan_again))
                    }
                }
            }
        }
    }
}
