package com.pdfsuite.app.ui.compress

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfsuite.app.R
import com.pdfsuite.app.pdf.displayName
import com.pdfsuite.app.pdf.toReadableSize

@Composable
fun CompressScreen(onBack: () -> Unit, viewModel: CompressViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.selectFile(it) }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.compress(it) }
    }

    val errorMessage = stringResource(R.string.compress_error)

    LaunchedEffect(uiState.status) {
        if (uiState.status == CompressStatus.ERROR) {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.compress_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (uiState.sourceUri == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.compress_empty))
                        Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Text(stringResource(R.string.compress_pick_file))
                        }
                    }
                }
            } else {
                val context = LocalContext.current
                Text(
                    text = uiState.sourceUri!!.displayName(context),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                uiState.originalSizeBytes?.let { size ->
                    Text(
                        text = stringResource(R.string.compress_original_size, size.toReadableSize()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                CompressionLevel.entries.forEach { level ->
                    val labelRes = when (level) {
                        CompressionLevel.HIGH -> R.string.compress_level_high
                        CompressionLevel.BALANCED -> R.string.compress_level_balanced
                        CompressionLevel.SMALL -> R.string.compress_level_small
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.level == level,
                                onClick = { viewModel.setLevel(level) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = uiState.level == level, onClick = { viewModel.setLevel(level) })
                        Text(stringResource(labelRes))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (uiState.status) {
                        CompressStatus.WORKING -> CircularProgressIndicator()
                        CompressStatus.DONE -> {
                            val original = uiState.originalSizeBytes
                            val compressed = uiState.compressedSizeBytes
                            if (original != null && compressed != null) {
                                val savedPercent = if (original > 0) {
                                    (100 - (compressed * 100 / original)).toInt().coerceIn(0, 100)
                                } else {
                                    0
                                }
                                Text(
                                    stringResource(
                                        R.string.compress_result,
                                        original.toReadableSize(),
                                        compressed.toReadableSize(),
                                        savedPercent,
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                        else -> Unit
                    }
                }

                Button(
                    onClick = { savePdf.launch("comprimido.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.status != CompressStatus.WORKING,
                ) {
                    Text(stringResource(R.string.compress_action))
                }
            }
        }
    }
}
