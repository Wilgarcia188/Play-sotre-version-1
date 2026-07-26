package com.pdfsuite.app.ui.annotate

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.AnnotateOperations
import com.pdfsuite.app.pdf.AnnotationTool
import com.pdfsuite.app.pdf.PageAnnotation
import com.pdfsuite.app.pdf.PageTextInfo
import com.pdfsuite.app.pdf.PdfRendererHelper
import com.pdfsuite.app.pdf.PdfTextChunk
import com.pdfsuite.app.pdf.PdfTextExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnnotateStatus { IDLE, LOADING, READY, SAVING, SUCCESS, ERROR }

data class AnnotateUiState(
    val sourceUri: Uri? = null,
    val pageCount: Int = 0,
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val currentPageText: PageTextInfo? = null,
    val annotationsByPage: Map<Int, List<PageAnnotation>> = emptyMap(),
    val selectedTool: AnnotationTool = AnnotationTool.PEN,
    val capturedSignature: Bitmap? = null,
    val status: AnnotateStatus = AnnotateStatus.IDLE,
)

class AnnotateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AnnotateUiState())
    val uiState: StateFlow<AnnotateUiState> = _uiState.asStateFlow()

    fun openPdf(uri: Uri, previewWidthPx: Int) {
        _uiState.value = AnnotateUiState(sourceUri = uri, status = AnnotateStatus.LOADING)
        viewModelScope.launch {
            val context = getApplication<Application>()
            val count = PdfRendererHelper.getPageCount(context, uri)
            val firstBitmap = PdfRendererHelper.renderPage(context, uri, 0, previewWidthPx)
            val textInfo = PdfTextExtractor.extractPageText(context, uri, 0)
            _uiState.update {
                it.copy(
                    pageCount = count,
                    currentPageIndex = 0,
                    currentPageBitmap = firstBitmap,
                    currentPageText = textInfo,
                    status = AnnotateStatus.READY,
                )
            }
        }
    }

    fun goToPage(index: Int, previewWidthPx: Int) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        if (index !in 0 until state.pageCount) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val bitmap = PdfRendererHelper.renderPage(context, source, index, previewWidthPx)
            val textInfo = PdfTextExtractor.extractPageText(context, source, index)
            _uiState.update {
                it.copy(currentPageIndex = index, currentPageBitmap = bitmap, currentPageText = textInfo)
            }
        }
    }

    fun selectTool(tool: AnnotationTool) {
        _uiState.update { it.copy(selectedTool = tool) }
    }

    fun setSignature(bitmap: Bitmap) {
        _uiState.update { it.copy(capturedSignature = bitmap) }
    }

    fun addAnnotation(annotation: PageAnnotation) {
        _uiState.update { state ->
            val updated = state.annotationsByPage.toMutableMap()
            updated[state.currentPageIndex] = updated[state.currentPageIndex].orEmpty() + annotation
            state.copy(annotationsByPage = updated)
        }
    }

    fun replaceText(original: PdfTextChunk, newText: String) {
        addAnnotation(PageAnnotation.TextReplacement(original, newText))
    }

    fun undoLastOnCurrentPage() {
        _uiState.update { state ->
            val current = state.annotationsByPage[state.currentPageIndex].orEmpty()
            if (current.isEmpty()) return@update state
            val updated = state.annotationsByPage.toMutableMap()
            updated[state.currentPageIndex] = current.dropLast(1)
            state.copy(annotationsByPage = updated)
        }
    }

    fun save(destination: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        _uiState.update { it.copy(status = AnnotateStatus.SAVING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = AnnotateOperations.applyAnnotations(context, source, destination, state.annotationsByPage)
            _uiState.update {
                it.copy(status = if (result.isSuccess) AnnotateStatus.SUCCESS else AnnotateStatus.ERROR)
            }
        }
    }
}
