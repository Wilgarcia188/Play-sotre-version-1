package com.pdfsuite.app.ui.organize

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfsuite.app.R

@Composable
fun OrganizeScreen(onBack: () -> Unit, viewModel: OrganizeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.openPdf(it, THUMB_WIDTH_PX) }
    }
    val saveOrganized = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.save(it) }
    }

    val successMessage = stringResource(R.string.organize_save_success)
    val errorMessage = stringResource(R.string.organize_save_error)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            OrganizeStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            OrganizeStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.organize_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.organize_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (uiState.status) {
                OrganizeStatus.LOADING -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                OrganizeStatus.IDLE -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.organize_empty))
                        Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                            Text(stringResource(R.string.organize_pick_file))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(uiState.pages) { index, page ->
                            OrganizePageRow(
                                page = page,
                                canMoveUp = index > 0,
                                canMoveDown = index < uiState.pages.size - 1,
                                onMoveUp = { viewModel.moveUp(index) },
                                onMoveDown = { viewModel.moveDown(index) },
                                onRotate = { viewModel.rotate(index) },
                                onRemove = { viewModel.removePage(index) },
                            )
                        }
                    }

                    if (uiState.status == OrganizeStatus.WORKING) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Button(
                        onClick = { saveOrganized.launch("documento_organizado.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.pages.isNotEmpty() && uiState.status != OrganizeStatus.WORKING,
                    ) {
                        Text(stringResource(R.string.organize_save))
                    }
                }
            }
        }
    }
}

private const val THUMB_WIDTH_PX = 200

@Composable
private fun OrganizePageRow(
    page: OrganizePageState,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = page.thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(4.dp))
                    .rotate(page.rotationDelta.toFloat()),
            )
            Text(
                text = stringResource(R.string.organize_page_label, page.originalIndex + 1),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            IconButton(onClick = onRotate) {
                Icon(Icons.Filled.RotateRight, contentDescription = stringResource(R.string.organize_rotate))
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.organize_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.organize_move_down))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.organize_delete))
            }
        }
    }
}
