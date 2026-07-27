package com.pdfsuite.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdfsuite.app.R

private data class HomeAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun HomeScreen(
    onOpenViewer: () -> Unit,
    onOpenMerge: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenOcr: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenWatermark: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenAnnotate: () -> Unit,
    onOpenOrganize: () -> Unit,
) {
    val actions = listOf(
        HomeAction(
            title = stringResource(R.string.home_action_scan),
            description = stringResource(R.string.home_action_scan_desc),
            icon = Icons.Filled.CameraAlt,
            onClick = onOpenScan,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_view),
            description = stringResource(R.string.home_action_view_desc),
            icon = Icons.Filled.MenuBook,
            onClick = onOpenViewer,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_merge),
            description = stringResource(R.string.home_action_merge_desc),
            icon = Icons.Filled.CallMerge,
            onClick = onOpenMerge,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_split),
            description = stringResource(R.string.home_action_split_desc),
            icon = Icons.Filled.CallSplit,
            onClick = onOpenSplit,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_ocr),
            description = stringResource(R.string.home_action_ocr_desc),
            icon = Icons.Filled.TextFields,
            onClick = onOpenOcr,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_compress),
            description = stringResource(R.string.home_action_compress_desc),
            icon = Icons.Filled.Compress,
            onClick = onOpenCompress,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_convert),
            description = stringResource(R.string.home_action_convert_desc),
            icon = Icons.Filled.SyncAlt,
            onClick = onOpenConvert,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_watermark),
            description = stringResource(R.string.home_action_watermark_desc),
            icon = Icons.Filled.Opacity,
            onClick = onOpenWatermark,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_annotate),
            description = stringResource(R.string.home_action_annotate_desc),
            icon = Icons.Filled.Edit,
            onClick = onOpenAnnotate,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_organize),
            description = stringResource(R.string.home_action_organize_desc),
            icon = Icons.Filled.Reorder,
            onClick = onOpenOrganize,
        ),
    )

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(actions) { action ->
                HomeActionCard(action)
            }
        }
    }
}

@Composable
private fun HomeActionCard(action: HomeAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(text = action.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
