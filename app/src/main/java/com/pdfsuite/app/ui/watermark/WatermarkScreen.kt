package com.pdfsuite.app.ui.watermark

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
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun WatermarkScreen(onBack: () -> Unit, viewModel: WatermarkViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.selectFile(it) }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.applyWatermark(it) }
    }

    val successMessage = stringResource(R.string.watermark_success)
    val errorMessage = stringResource(R.string.watermark_error)
    val needTextMessage = stringResource(R.string.watermark_need_text)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            WatermarkStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            WatermarkStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            WatermarkStatus.NEED_TEXT -> snackbarHostState.showSnackbar(needTextMessage)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watermark_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.watermark_pick_file))
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
                        Text(stringResource(R.string.watermark_empty))
                        Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Text(stringResource(R.string.watermark_pick_file))
                        }
                    }
                }
            } else {
                Text(
                    text = uiState.sourceUri!!.displayName(context),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = { viewModel.setText(it) },
                    label = { Text(stringResource(R.string.watermark_text_label)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true,
                )

                Text(stringResource(R.string.watermark_opacity_label), style = MaterialTheme.typography.bodyLarge)
                WatermarkOpacity.entries.forEach { opacity ->
                    val labelRes = when (opacity) {
                        WatermarkOpacity.SUBTLE -> R.string.watermark_opacity_subtle
                        WatermarkOpacity.MEDIUM -> R.string.watermark_opacity_medium
                        WatermarkOpacity.STRONG -> R.string.watermark_opacity_strong
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.opacity == opacity,
                                onClick = { viewModel.setOpacity(opacity) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = uiState.opacity == opacity, onClick = { viewModel.setOpacity(opacity) })
                        Text(stringResource(labelRes))
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (uiState.status == WatermarkStatus.WORKING) {
                        CircularProgressIndicator()
                    }
                }

                Button(
                    onClick = { savePdf.launch("con_marca_de_agua.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.status != WatermarkStatus.WORKING,
                ) {
                    Text(stringResource(R.string.watermark_action))
                }
            }
        }
    }
}
