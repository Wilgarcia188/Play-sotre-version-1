package com.pdfsuite.app.ui.convert

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfsuite.app.R
import com.pdfsuite.app.pdf.displayName

@Composable
fun ConvertScreen(onBack: () -> Unit, viewModel: ConvertViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.convert_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.convert_tab_images_to_pdf)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.convert_tab_pdf_to_images)) },
                )
            }
            if (selectedTab == 0) {
                ImagesToPdfTab(viewModel, snackbarHostState)
            } else {
                PdfToImagesTab(viewModel, snackbarHostState)
            }
        }
    }
}

@Composable
private fun ImagesToPdfTab(viewModel: ConvertViewModel, snackbarHostState: SnackbarHostState) {
    val uiState by viewModel.imagesToPdfState.collectAsState()
    val context = LocalContext.current

    val addImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addImages(uris)
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.createPdf(it) }
    }

    val successMessage = stringResource(R.string.convert_images_success)
    val errorMessage = stringResource(R.string.convert_images_error)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            ImagesToPdfStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            ImagesToPdfStatus.ERROR, ImagesToPdfStatus.NEED_IMAGE -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { addImages.launch(arrayOf("image/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.convert_add_images))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.images.isEmpty()) {
                Text(
                    text = stringResource(R.string.convert_images_empty),
                    modifier = Modifier.padding(top = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(uiState.images) { index, uri ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = uri.displayName(context),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                IconButton(onClick = { viewModel.moveImageUp(index) }, enabled = index > 0) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.merge_move_up))
                                }
                                IconButton(
                                    onClick = { viewModel.moveImageDown(index) },
                                    enabled = index < uiState.images.size - 1,
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.merge_move_down))
                                }
                                IconButton(onClick = { viewModel.removeImage(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.convert_remove))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.status == ImagesToPdfStatus.WORKING) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = { savePdf.launch("imagenes.pdf") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.images.isNotEmpty() && uiState.status != ImagesToPdfStatus.WORKING,
        ) {
            Text(stringResource(R.string.convert_create_pdf))
        }
    }
}

@Composable
private fun PdfToImagesTab(viewModel: ConvertViewModel, snackbarHostState: SnackbarHostState) {
    val uiState by viewModel.pdfToImagesState.collectAsState()

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.selectPdf(it) }
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { viewModel.exportPagesAsImages(it) }
    }

    val successMessage = stringResource(R.string.convert_export_success)
    val errorMessage = stringResource(R.string.convert_export_error)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            PdfToImagesStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            PdfToImagesStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (uiState.sourceUri == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.convert_pdf_empty))
                    Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Text(stringResource(R.string.convert_pdf_pick_file))
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (uiState.status == PdfToImagesStatus.WORKING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        val progress = if (uiState.totalPages > 0) {
                            uiState.processedPages.toFloat() / uiState.totalPages
                        } else {
                            0f
                        }
                        Text(stringResource(R.string.convert_export_progress, uiState.processedPages, uiState.totalPages))
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Button(
                onClick = { pickFolder.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.status != PdfToImagesStatus.WORKING,
            ) {
                Text(stringResource(R.string.convert_pick_folder))
            }
        }
    }
}
