package com.pdfsuite.app.ui.annotate

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfsuite.app.R
import com.pdfsuite.app.pdf.AnnotationTool
import com.pdfsuite.app.pdf.NormalizedPoint
import com.pdfsuite.app.pdf.PageAnnotation
import com.pdfsuite.app.pdf.PageTextInfo
import com.pdfsuite.app.pdf.PdfTextChunk
import com.pdfsuite.app.pdf.TEXT_REPLACEMENT_DESCENT_PADDING_FRACTION
import com.pdfsuite.app.pdf.TEXT_REPLACEMENT_TOP_PADDING_FRACTION
import kotlinx.coroutines.launch

@Composable
fun AnnotateScreen(onBack: () -> Unit, viewModel: AnnotateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val previewWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var pendingTextPosition by remember { mutableStateOf<NormalizedPoint?>(null) }
    var pendingSignaturePosition by remember { mutableStateOf<NormalizedPoint?>(null) }
    var pendingEditTextChunk by remember { mutableStateOf<PdfTextChunk?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var lastPageBoxSize by remember { mutableStateOf(IntSize.Zero) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.openPdf(it, previewWidthPx) }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let { viewModel.save(it) }
    }

    val successMessage = stringResource(R.string.annotate_save_success)
    val errorMessage = stringResource(R.string.annotate_save_error)
    val editTextNotFoundMessage = stringResource(R.string.annotate_edit_text_not_found)

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            AnnotateStatus.SUCCESS -> snackbarHostState.showSnackbar(successMessage)
            AnnotateStatus.ERROR -> snackbarHostState.showSnackbar(errorMessage)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.annotate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.annotate_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val bitmap = uiState.currentPageBitmap
            if (uiState.sourceUri == null || bitmap == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (uiState.status == AnnotateStatus.LOADING) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.annotate_empty))
                            Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                                Text(stringResource(R.string.annotate_pick_file))
                            }
                        }
                    }
                }
            } else {
                ToolBar(
                    selectedTool = uiState.selectedTool,
                    onSelectTool = { viewModel.selectTool(it) },
                    onUndo = { viewModel.undoLastOnCurrentPage() },
                    currentPage = uiState.currentPageIndex,
                    pageCount = uiState.pageCount,
                    onPrevPage = { viewModel.goToPage(uiState.currentPageIndex - 1, previewWidthPx) },
                    onNextPage = { viewModel.goToPage(uiState.currentPageIndex + 1, previewWidthPx) },
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnnotationCanvas(
                        bitmap = bitmap,
                        tool = uiState.selectedTool,
                        annotations = uiState.annotationsByPage[uiState.currentPageIndex].orEmpty(),
                        pageTextInfo = uiState.currentPageText,
                        onSizeChanged = { lastPageBoxSize = it },
                        onStrokeCommitted = { points, tool ->
                            viewModel.addAnnotation(PageAnnotation.Stroke(points, tool))
                        },
                        onTapForText = { position -> pendingTextPosition = position },
                        onTapForSignature = { position ->
                            val signature = uiState.capturedSignature
                            val boxSize = lastPageBoxSize
                            if (signature != null && boxSize.width > 0 && boxSize.height > 0) {
                                val widthFraction = 0.32f
                                val aspect = signature.height.toFloat() / signature.width.toFloat()
                                val heightFraction = widthFraction * (boxSize.width.toFloat() / boxSize.height.toFloat()) * aspect
                                viewModel.addAnnotation(
                                    PageAnnotation.Signature(position, signature, widthFraction, heightFraction),
                                )
                            } else {
                                pendingSignaturePosition = position
                                showSignaturePad = true
                            }
                        },
                        onTapForEditText = { position ->
                            val info = uiState.currentPageText
                            val chunk = info?.let { findTappedChunk(position, it) }
                            if (chunk != null) {
                                pendingEditTextChunk = chunk
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(editTextNotFoundMessage) }
                            }
                        },
                    )
                }

                if (uiState.selectedTool == AnnotationTool.SIGNATURE && uiState.capturedSignature != null) {
                    OutlinedButton(
                        onClick = { showSignaturePad = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text(stringResource(R.string.annotate_redo_signature))
                    }
                }

                Button(
                    onClick = { savePdf.launch("documento_anotado.pdf") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = uiState.status != AnnotateStatus.SAVING,
                ) {
                    if (uiState.status == AnnotateStatus.SAVING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.annotate_save))
                    }
                }
            }
        }
    }

    pendingTextPosition?.let { position ->
        TextEntryDialog(
            title = stringResource(R.string.annotate_text_dialog_title),
            onConfirm = { text ->
                if (text.isNotBlank()) {
                    viewModel.addAnnotation(PageAnnotation.TextNote(position, text))
                }
                pendingTextPosition = null
            },
            onDismiss = { pendingTextPosition = null },
        )
    }

    pendingEditTextChunk?.let { chunk ->
        TextEntryDialog(
            title = stringResource(R.string.annotate_edit_text_dialog_title),
            initialText = chunk.text,
            onConfirm = { text ->
                viewModel.replaceText(chunk, text)
                pendingEditTextChunk = null
            },
            onDismiss = { pendingEditTextChunk = null },
        )
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onAccept = { bitmap ->
                viewModel.setSignature(bitmap)
                showSignaturePad = false
                val position = pendingSignaturePosition
                val boxSize = lastPageBoxSize
                if (position != null && boxSize.width > 0 && boxSize.height > 0) {
                    val widthFraction = 0.32f
                    val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
                    val heightFraction = widthFraction * (boxSize.width.toFloat() / boxSize.height.toFloat()) * aspect
                    viewModel.addAnnotation(PageAnnotation.Signature(position, bitmap, widthFraction, heightFraction))
                }
                pendingSignaturePosition = null
            },
            onDismiss = {
                showSignaturePad = false
                pendingSignaturePosition = null
            },
        )
    }
}

@Composable
private fun ToolBar(
    selectedTool: AnnotationTool,
    onSelectTool: (AnnotationTool) -> Unit,
    onUndo: () -> Unit,
    currentPage: Int,
    pageCount: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                icon = Icons.Filled.Edit,
                selected = selectedTool == AnnotationTool.PEN,
                contentDescription = stringResource(R.string.annotate_tool_pen),
                onClick = { onSelectTool(AnnotationTool.PEN) },
            )
            ToolButton(
                icon = Icons.Filled.Brush,
                selected = selectedTool == AnnotationTool.HIGHLIGHTER,
                contentDescription = stringResource(R.string.annotate_tool_highlighter),
                onClick = { onSelectTool(AnnotationTool.HIGHLIGHTER) },
            )
            ToolButton(
                icon = Icons.Filled.TextFields,
                selected = selectedTool == AnnotationTool.TEXT,
                contentDescription = stringResource(R.string.annotate_tool_text),
                onClick = { onSelectTool(AnnotationTool.TEXT) },
            )
            ToolButton(
                icon = Icons.Filled.Draw,
                selected = selectedTool == AnnotationTool.SIGNATURE,
                contentDescription = stringResource(R.string.annotate_tool_signature),
                onClick = { onSelectTool(AnnotationTool.SIGNATURE) },
            )
            ToolButton(
                icon = Icons.Filled.FindReplace,
                selected = selectedTool == AnnotationTool.EDIT_TEXT,
                contentDescription = stringResource(R.string.annotate_tool_edit_text),
                onClick = { onSelectTool(AnnotationTool.EDIT_TEXT) },
            )
            IconButton(onClick = onUndo) {
                Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.annotate_undo))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevPage, enabled = currentPage > 0) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = null)
            }
            Text(stringResource(R.string.annotate_page_of, currentPage + 1, pageCount))
            IconButton(onClick = onNextPage, enabled = currentPage < pageCount - 1) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnnotationCanvas(
    bitmap: Bitmap,
    tool: AnnotationTool,
    annotations: List<PageAnnotation>,
    pageTextInfo: PageTextInfo?,
    onSizeChanged: (IntSize) -> Unit,
    onStrokeCommitted: (List<NormalizedPoint>, AnnotationTool) -> Unit,
    onTapForText: (NormalizedPoint) -> Unit,
    onTapForSignature: (NormalizedPoint) -> Unit,
    onTapForEditText: (NormalizedPoint) -> Unit,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var currentStrokePoints by remember(tool) { mutableStateOf(listOf<Offset>()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .onSizeChanged {
                boxSize = it
                onSizeChanged(it)
            }
            .pointerInputForTool(
                tool = tool,
                onDragStart = { offset -> currentStrokePoints = listOf(offset) },
                onDrag = { offset -> currentStrokePoints = currentStrokePoints + offset },
                onDragEnd = {
                    val size = boxSize
                    if (currentStrokePoints.size >= 2 && size.width > 0 && size.height > 0) {
                        val normalized = currentStrokePoints.map {
                            NormalizedPoint(it.x / size.width, it.y / size.height)
                        }
                        onStrokeCommitted(normalized, tool)
                    }
                    currentStrokePoints = emptyList()
                },
                onTap = { offset ->
                    val size = boxSize
                    if (size.width > 0 && size.height > 0) {
                        val normalized = NormalizedPoint(offset.x / size.width, offset.y / size.height)
                        when (tool) {
                            AnnotationTool.TEXT -> onTapForText(normalized)
                            AnnotationTool.EDIT_TEXT -> onTapForEditText(normalized)
                            else -> onTapForSignature(normalized)
                        }
                    }
                },
            ),
    ) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())

        Canvas(modifier = Modifier.fillMaxSize()) {
            annotations.forEach { annotation ->
                when (annotation) {
                    is PageAnnotation.Stroke -> drawStroke(annotation.points, annotation.tool, size.width, size.height)
                    is PageAnnotation.TextNote -> drawTextNote(annotation, size.width, size.height)
                    is PageAnnotation.Signature -> {
                        drawImage(
                            image = annotation.bitmap.asImageBitmap(),
                            dstOffset = IntOffset(
                                (annotation.position.x * size.width).toInt(),
                                (annotation.position.y * size.height).toInt(),
                            ),
                            dstSize = IntSize(
                                (annotation.widthFraction * size.width).toInt().coerceAtLeast(1),
                                (annotation.heightFraction * size.height).toInt().coerceAtLeast(1),
                            ),
                        )
                    }
                    is PageAnnotation.TextReplacement -> {
                        pageTextInfo?.let { info -> drawTextReplacement(annotation, info, size.width, size.height) }
                    }
                }
            }

            if (currentStrokePoints.size >= 2) {
                drawLivePath(currentStrokePoints, tool)
            }
        }
    }
}

private fun DrawScope.drawStroke(points: List<NormalizedPoint>, tool: AnnotationTool, widthPx: Float, heightPx: Float) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val offset = Offset(point.x * widthPx, point.y * heightPx)
        if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
    }
    drawStrokePath(path, tool, widthPx)
}

private fun DrawScope.drawLivePath(points: List<Offset>, tool: AnnotationTool) {
    val path = Path()
    points.forEachIndexed { index, offset ->
        if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
    }
    drawStrokePath(path, tool, size.width)
}

private fun DrawScope.drawStrokePath(path: Path, tool: AnnotationTool, widthPx: Float) {
    val isHighlighter = tool == AnnotationTool.HIGHLIGHTER
    drawPath(
        path = path,
        color = if (isHighlighter) Color(0xFFFFEB3B).copy(alpha = 0.4f) else Color(0xFFDC0000),
        style = Stroke(
            width = if (isHighlighter) widthPx * 0.03f else widthPx * 0.006f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawTextNote(note: PageAnnotation.TextNote, widthPx: Float, heightPx: Float) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = widthPx * 0.035f
        isAntiAlias = true
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        note.text,
        note.position.x * widthPx,
        note.position.y * heightPx,
        paint,
    )
}

/** Mirrors [com.pdfsuite.app.pdf.AnnotateOperations]'s drawTextReplacement, but in on-screen
 * pixel space, converting the chunk's absolute PDF-point box via [info]'s page size. */
private fun DrawScope.drawTextReplacement(
    replacement: PageAnnotation.TextReplacement,
    info: PageTextInfo,
    widthPx: Float,
    heightPx: Float,
) {
    val chunk = replacement.original
    val descentPaddingPt = chunk.height * TEXT_REPLACEMENT_DESCENT_PADDING_FRACTION
    val topPaddingPt = chunk.height * TEXT_REPLACEMENT_TOP_PADDING_FRACTION
    val boxTopPt = info.pageHeightPt - (chunk.baselineY + chunk.height + topPaddingPt)

    val left = (chunk.x / info.pageWidthPt) * widthPx
    val top = (boxTopPt / info.pageHeightPt) * heightPx
    val boxWidth = (chunk.width / info.pageWidthPt) * widthPx
    val boxHeight = ((chunk.height + descentPaddingPt + topPaddingPt) / info.pageHeightPt) * heightPx

    drawRect(color = Color.White, topLeft = Offset(left, top), size = Size(boxWidth, boxHeight))

    if (replacement.newText.isNotBlank()) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = boxHeight * 0.7f
            isAntiAlias = true
        }
        // Where the baseline sits within the covered box, as a fraction of its height -
        // derived from the same padding constants used to compute boxHeight, so this
        // always matches regardless of their values.
        val baselineFraction = (chunk.height + topPaddingPt) / (chunk.height + descentPaddingPt + topPaddingPt)
        drawContext.canvas.nativeCanvas.drawText(replacement.newText, left, top + boxHeight * baselineFraction, paint)
    }
}

private fun findTappedChunk(tap: NormalizedPoint, info: PageTextInfo): PdfTextChunk? {
    val tapXPt = tap.x * info.pageWidthPt
    val tapYPt = info.pageHeightPt - tap.y * info.pageHeightPt
    return info.chunks.firstOrNull { chunk ->
        val padding = (chunk.height * 0.3f).coerceAtLeast(4f)
        tapXPt in (chunk.x - padding)..(chunk.x + chunk.width + padding) &&
            tapYPt in (chunk.baselineY - padding)..(chunk.baselineY + chunk.height + padding)
    }
}

private fun Modifier.pointerInputForTool(
    tool: AnnotationTool,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Offset) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(tool) {
        when (tool) {
            AnnotationTool.PEN, AnnotationTool.HIGHLIGHTER -> {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDrag = { change, _ -> onDrag(change.position) },
                )
            }
            AnnotationTool.TEXT, AnnotationTool.SIGNATURE, AnnotationTool.EDIT_TEXT -> {
                detectTapGestures(onTap = onTap)
            }
        }
    },
)

@Composable
private fun TextEntryDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialText: String = "",
) {
    var text by remember { mutableStateOf(initialText) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.annotate_text_dialog_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.annotate_cancel))
                    }
                    Button(onClick = { onConfirm(text) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.annotate_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignaturePadDialog(onAccept: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.annotate_signature_hint), style = MaterialTheme.typography.bodyLarge)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(top = 12.dp)
                        .background(Color.White)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> currentPath = listOf(offset) },
                                onDrag = { change, _ -> currentPath = currentPath + change.position },
                                onDragEnd = {
                                    if (currentPath.size >= 2) paths.add(currentPath)
                                    currentPath = emptyList()
                                },
                            )
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        (paths + listOf(currentPath)).forEach { pathPoints ->
                            if (pathPoints.size < 2) return@forEach
                            val path = Path()
                            pathPoints.forEachIndexed { index, offset ->
                                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                            }
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            paths.clear()
                            currentPath = emptyList()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.annotate_signature_clear))
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.annotate_cancel))
                    }
                    Button(
                        onClick = {
                            if (canvasSize.width > 0 && canvasSize.height > 0 && paths.isNotEmpty()) {
                                onAccept(renderSignatureBitmap(paths.toList(), canvasSize))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.annotate_signature_accept))
                    }
                }
            }
        }
    }
}

private fun renderSignatureBitmap(paths: List<List<Offset>>, size: IntSize): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    paths.forEach { pathPoints ->
        if (pathPoints.size < 2) return@forEach
        val androidPath = android.graphics.Path()
        pathPoints.forEachIndexed { index, offset ->
            if (index == 0) androidPath.moveTo(offset.x, offset.y) else androidPath.lineTo(offset.x, offset.y)
        }
        canvas.drawPath(androidPath, paint)
    }
    return bitmap
}
