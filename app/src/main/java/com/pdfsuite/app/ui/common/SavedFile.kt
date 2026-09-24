package com.pdfsuite.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pdfsuite.app.R
import com.pdfsuite.app.pdf.PdfRendererHelper
import com.pdfsuite.app.pdf.displayName
import com.pdfsuite.app.pdf.fileSizeBytes
import com.pdfsuite.app.pdf.toReadableSize

const val MIME_PDF = "application/pdf"
const val MIME_TEXT = "text/plain"

/**
 * Remembers the file a screen is about to write to, so that once the operation
 * succeeds the screen can offer to review, share or open it instead of just
 * flashing a snackbar.
 */
@Stable
class SavedFileHolder {

    var uri by mutableStateOf<Uri?>(null)
        private set

    var isVisible by mutableStateOf(false)
        private set

    /** Called with the destination picked in the system file dialog, before writing to it. */
    fun onDestinationChosen(destination: Uri) {
        uri = destination
        isVisible = false
    }

    fun show() {
        if (uri != null) isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }

    companion object {
        val Saver = listSaver<SavedFileHolder, Any?>(
            save = { listOf(it.uri?.toString(), it.isVisible) },
            restore = { saved ->
                SavedFileHolder().apply {
                    uri = (saved[0] as String?)?.let(Uri::parse)
                    isVisible = saved[1] as Boolean
                }
            },
        )
    }
}

@Composable
fun rememberSavedFileHolder(): SavedFileHolder =
    rememberSaveable(saver = SavedFileHolder.Saver) { SavedFileHolder() }

/**
 * Shows [SavedFileSheet] for the file tracked by [holder] while it is visible.
 * [onOpenInViewer] is null for files the in-app viewer cannot render (e.g. plain text).
 */
@Composable
fun SavedFileSheetHost(
    holder: SavedFileHolder,
    mimeType: String = MIME_PDF,
    onOpenInViewer: ((Uri) -> Unit)? = null,
) {
    val uri = holder.uri
    if (holder.isVisible && uri != null) {
        SavedFileSheet(
            uri = uri,
            mimeType = mimeType,
            onOpenInViewer = onOpenInViewer?.let { open ->
                {
                    holder.dismiss()
                    open(uri)
                }
            },
            onDismiss = { holder.dismiss() },
        )
    }
}

@Composable
private fun SavedFileSheet(
    uri: Uri,
    mimeType: String,
    onOpenInViewer: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val info = rememberSavedFileInfo(uri, mimeType)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.saved_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                info.thumbnail?.let { thumbnail ->
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 56.dp, height = 72.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                }
                Column(modifier = Modifier.padding(start = if (info.thumbnail != null) 12.dp else 0.dp)) {
                    Text(text = info.name, style = MaterialTheme.typography.bodyLarge)
                    val details = listOfNotNull(
                        info.sizeBytes?.toReadableSize(),
                        info.pageCount?.let { stringResource(R.string.saved_pages, it) },
                    ).joinToString(" · ")
                    if (details.isNotEmpty()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (onOpenInViewer != null) {
                Button(onClick = onOpenInViewer, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.saved_review))
                }
            }
            Button(
                onClick = { context.shareFile(uri, mimeType) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.saved_share))
            }
            OutlinedButton(
                onClick = { context.openFileWith(uri, mimeType) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.saved_open_with))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.saved_done))
            }
        }
    }
}

private data class SavedFileInfo(
    val name: String,
    val sizeBytes: Long?,
    val pageCount: Int?,
    val thumbnail: Bitmap?,
)

@Composable
private fun rememberSavedFileInfo(uri: Uri, mimeType: String): SavedFileInfo {
    val context = LocalContext.current
    val initial = SavedFileInfo(name = stringResource(R.string.loading), sizeBytes = null, pageCount = null, thumbnail = null)
    val state by produceState(initialValue = initial, uri, mimeType) {
        val name = uri.displayName(context)
        val size = uri.fileSizeBytes(context)
        value = SavedFileInfo(name = name, sizeBytes = size, pageCount = null, thumbnail = null)
        if (mimeType == MIME_PDF) {
            val pages = runCatching { PdfRendererHelper.getPageCount(context, uri) }.getOrNull()
            val thumbnail = runCatching {
                PdfRendererHelper.renderPage(context, uri, 0, THUMBNAIL_WIDTH_PX)
            }.getOrNull()
            value = SavedFileInfo(name = name, sizeBytes = size, pageCount = pages, thumbnail = thumbnail)
        }
    }
    return state
}

private const val THUMBNAIL_WIDTH_PX = 220

fun Context.shareFile(uri: Uri, mimeType: String = MIME_PDF) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startChooser(intent, getString(R.string.saved_share))
}

fun Context.openFileWith(uri: Uri, mimeType: String = MIME_PDF) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startChooser(intent, getString(R.string.saved_open_with))
}

private fun Context.startChooser(intent: Intent, title: String) {
    val chooser = Intent.createChooser(intent, title).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.saved_no_app, Toast.LENGTH_SHORT).show()
    }
}
