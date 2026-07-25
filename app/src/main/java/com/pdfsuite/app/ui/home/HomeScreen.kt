package com.pdfsuite.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
) {
    val actions = listOf(
        HomeAction(
            title = stringResource(R.string.home_action_view),
            description = stringResource(R.string.home_action_view_desc),
            icon = Icons.Filled.MenuBook,
            onClick = onOpenViewer,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_merge),
            description = stringResource(R.string.home_action_merge_desc),
            icon = Icons.Filled.Merge,
            onClick = onOpenMerge,
        ),
        HomeAction(
            title = stringResource(R.string.home_action_split),
            description = stringResource(R.string.home_action_split_desc),
            icon = Icons.Filled.CallSplit,
            onClick = onOpenSplit,
        ),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(actions) { action ->
                    HomeActionCard(action)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(action: HomeAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(text = action.title, style = MaterialTheme.typography.titleLarge)
                Text(text = action.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
