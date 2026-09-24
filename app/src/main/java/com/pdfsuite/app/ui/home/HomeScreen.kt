package com.pdfsuite.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfsuite.app.R
import com.pdfsuite.app.ui.theme.ToolAccent

private data class HomeAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: ToolAccent,
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
            title = stringResource(R.string.home_action_view),
            description = stringResource(R.string.home_action_view_desc),
            icon = Icons.Filled.MenuBook,
            accent = ToolAccent.INDIGO,
            onClick = onOpenViewer,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_merge),
            description = stringResource(R.string.home_action_merge_desc),
            icon = Icons.Filled.CallMerge,
            accent = ToolAccent.SKY,
            onClick = onOpenMerge,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_split),
            description = stringResource(R.string.home_action_split_desc),
            icon = Icons.Filled.CallSplit,
            accent = ToolAccent.SKY,
            onClick = onOpenSplit,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_organize),
            description = stringResource(R.string.home_action_organize_desc),
            icon = Icons.Filled.Reorder,
            accent = ToolAccent.TEAL,
            onClick = onOpenOrganize,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_annotate),
            description = stringResource(R.string.home_action_annotate_desc),
            icon = Icons.Filled.Edit,
            accent = ToolAccent.VIOLET,
            onClick = onOpenAnnotate,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_ocr),
            description = stringResource(R.string.home_action_ocr_desc),
            icon = Icons.Filled.TextFields,
            accent = ToolAccent.VIOLET,
            onClick = onOpenOcr,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_compress),
            description = stringResource(R.string.home_action_compress_desc),
            icon = Icons.Filled.Compress,
            accent = ToolAccent.AMBER,
            onClick = onOpenCompress,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_convert),
            description = stringResource(R.string.home_action_convert_desc),
            icon = Icons.Filled.SyncAlt,
            accent = ToolAccent.AMBER,
            onClick = onOpenConvert,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_watermark),
            description = stringResource(R.string.home_action_watermark_desc),
            icon = Icons.Filled.Opacity,
            accent = ToolAccent.CORAL,
            onClick = onOpenWatermark,
        ),
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ScanHeroCard(onClick = onOpenScan)
            }

            items(actions) { action ->
                ToolTile(action)
            }
        }
    }
}

@Composable
private fun ScanHeroCard(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary)))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_action_scan),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_action_scan_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolTile(action: HomeAction) {
    val accent = if (isSystemInDarkTheme()) action.accent.dark else action.accent.light
    Card(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
