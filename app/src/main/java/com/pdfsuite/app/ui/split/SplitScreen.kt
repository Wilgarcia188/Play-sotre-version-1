package com.pdfsuite.app.ui.split

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfsuite.app.R

@Composable
fun SplitScreen(onBack: () -> Unit, viewModel: SplitViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.openPdf(it, THUMB_WIDTH_PX) }
    }
    val saveExtracted = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.extractSelected(it) }
    }
    val saveRest = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.exportRest(it) }
    }

    val successMessage = stringResource(R.string.split_success)
    val errorMessage = stringResource(R.string.split_error)
    val needOneMessage = stringResource(R.string.split_need_one)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            SplitStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            SplitStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            SplitStatus.NEED_ONE -> snackbarHostState.showSnackbar(needOneMessage)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.split_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.split_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (uiState.status) {
                SplitStatus.LOADING -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                SplitStatus.IDLE -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.split_empty))
                        Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Text(stringResource(R.string.split_pick_file))
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { viewModel.selectAll() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.split_select_all))
                        }
                        OutlinedButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.split_clear))
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.pages.size) { index ->
                            PageThumbnail(
                                bitmap = uiState.pages[index].asImageBitmap(),
                                pageNumber = index + 1,
                                selected = index in uiState.selected,
                                onClick = { viewModel.togglePage(index) },
                            )
                        }
                    }

                    if (uiState.status == SplitStatus.WORKING) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Button(
                        onClick = { saveExtracted.launch("paginas_extraidas.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.status != SplitStatus.WORKING,
                    ) {
                        Text(stringResource(R.string.split_extract_selected))
                    }
                    Button(
                        onClick = { saveRest.launch("paginas_restantes.pdf") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = uiState.status != SplitStatus.WORKING,
                    ) {
                        Text(stringResource(R.string.split_export_rest))
                    }
                }
            }
        }
    }
}

private const val THUMB_WIDTH_PX = 300

@Composable
private fun PageThumbnail(
    bitmap: ImageBitmap,
    pageNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(4.dp).align(Alignment.TopEnd),
                )
            }
        }
        Text(
            text = pageNumber.toString(),
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
