package com.pdfsuite.app.ui.organize

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfOperations
import com.pdfsuite.app.pdf.PdfRendererHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrganizeStatus { IDLE, LOADING, READY, WORKING, SUCCESS, ERROR }

/** One page in the working arrangement. [originalIndex] is this page's position in the
 * source PDF (0-based) and never changes; the entry's position in [OrganizeUiState.pages]
 * is its new position, and [rotationDelta] is extra clockwise rotation (0/90/180/270) on
 * top of whatever rotation the page already had. */
data class OrganizePageState(
    val originalIndex: Int,
    val thumbnail: Bitmap,
    val rotationDelta: Int = 0,
)

data class OrganizeUiState(
    val sourceUri: Uri? = null,
    val pages: List<OrganizePageState> = emptyList(),
    val status: OrganizeStatus = OrganizeStatus.IDLE,
)

class OrganizeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OrganizeUiState())
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    fun openPdf(uri: Uri, thumbWidthPx: Int) {
        _uiState.value = OrganizeUiState(sourceUri = uri, status = OrganizeStatus.LOADING)
        viewModelScope.launch {
            val context = getApplication<Application>()
            runCatching {
                PdfRendererHelper.renderAllPages(context, uri, thumbWidthPx)
            }.onSuccess { bitmaps ->
                val pages = bitmaps.mapIndexed { index, bitmap -> OrganizePageState(index, bitmap) }
                _uiState.value = OrganizeUiState(sourceUri = uri, pages = pages, status = OrganizeStatus.READY)
            }.onFailure {
                _uiState.value = OrganizeUiState(sourceUri = uri, status = OrganizeStatus.ERROR)
            }
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val pages = state.pages.toMutableList()
            val page = pages.removeAt(index)
            pages.add(index - 1, page)
            state.copy(pages = pages)
        }
    }

    fun moveDown(index: Int) {
        _uiState.update { state ->
            if (index >= state.pages.lastIndex) return@update state
            val pages = state.pages.toMutableList()
            val page = pages.removeAt(index)
            pages.add(index + 1, page)
            state.copy(pages = pages)
        }
    }

    fun rotate(index: Int) {
        _uiState.update { state ->
            val pages = state.pages.toMutableList()
            val page = pages[index]
            pages[index] = page.copy(rotationDelta = (page.rotationDelta + 90) % 360)
            state.copy(pages = pages)
        }
    }

    fun removePage(index: Int) {
        _uiState.update { state ->
            val pages = state.pages.toMutableList()
            pages.removeAt(index)
            state.copy(pages = pages)
        }
    }

    fun save(destination: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        if (state.pages.isEmpty()) return
        _uiState.update { it.copy(status = OrganizeStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val plan = state.pages.map { PdfOperations.PageRotation(it.originalIndex, it.rotationDelta) }
            val result = PdfOperations.reorganizePages(context, source, destination, plan)
            _uiState.update {
                it.copy(status = if (result.isSuccess) OrganizeStatus.SUCCESS else OrganizeStatus.ERROR)
            }
        }
    }
}
