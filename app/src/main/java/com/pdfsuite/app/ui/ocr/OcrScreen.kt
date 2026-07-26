package com.pdfsuite.app.ui.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.pdfsuite.app.R
import kotlinx.coroutines.launch

@Composable
fun OcrScreen(onBack: () -> Unit, viewModel: OcrViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.startOcr(it) }
    }
    val saveText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let { viewModel.exportText(it) }
    }

    val copiedMessage = stringResource(R.string.ocr_copied)
    val exportedMessage = stringResource(R.string.ocr_export_success)
    val exportErrorMessage = stringResource(R.string.ocr_export_error)
    val errorMessage = stringResource(R.string.ocr_error)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            OcrStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            OcrStatus.EXPORTED -> snackbarHostState.showSnackbar(exportedMessage)
            OcrStatus.EXPORT_ERROR -> snackbarHostState.showSnackbar(exportErrorMessage)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.ocr_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (uiState.status) {
                OcrStatus.IDLE -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.ocr_empty))
                        Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Text(stringResource(R.string.ocr_pick_file))
                        }
                    }
                }
                else -> {
                    if (uiState.status == OcrStatus.PROCESSING) {
                        val progress = if (uiState.totalPages > 0) {
                            uiState.processedPages.toFloat() / uiState.totalPages
                        } else {
                            0f
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                            Text(stringResource(R.string.ocr_progress, uiState.processedPages, uiState.totalPages))
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (uiState.recognizedText.isBlank()) {
                            if (uiState.status == OcrStatus.DONE) {
                                Text(stringResource(R.string.ocr_no_text))
                            }
                        } else {
                            SelectionContainer {
                                Text(
                                    text = uiState.recognizedText,
                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                )
                            }
                        }
                    }

                    if (uiState.recognizedText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PDF Suite OCR", uiState.recognizedText))
                                    coroutineScope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.ocr_copy))
                            }
                            Button(
                                onClick = { saveText.launch("texto_reconocido.txt") },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.ocr_save_txt))
                            }
                        }
                    }
                }
            }
        }
    }
}
