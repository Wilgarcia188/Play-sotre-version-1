package com.pdfsuite.app.ui.split

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

enum class SplitStatus { IDLE, LOADING, READY, WORKING, SUCCESS, ERROR, NEED_ONE }

data class SplitUiState(
    val sourceUri: Uri? = null,
    val pages: List<Bitmap> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val status: SplitStatus = SplitStatus.IDLE,
)

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    fun openPdf(uri: Uri, thumbWidthPx: Int) {
        _uiState.value = SplitUiState(sourceUri = uri, status = SplitStatus.LOADING)
        viewModelScope.launch {
            val context = getApplication<Application>()
            runCatching {
                PdfRendererHelper.renderAllPages(context, uri, thumbWidthPx)
            }.onSuccess { pages ->
                _uiState.value = SplitUiState(sourceUri = uri, pages = pages, status = SplitStatus.READY)
            }.onFailure {
                _uiState.value = SplitUiState(sourceUri = uri, status = SplitStatus.ERROR)
            }
        }
    }

    fun togglePage(index: Int) {
        _uiState.update { state ->
            val selected = state.selected.toMutableSet()
            if (!selected.add(index)) selected.remove(index)
            state.copy(selected = selected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selected = it.pages.indices.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selected = emptySet()) }
    }

    fun extractSelected(destination: Uri) = runExport(destination) { it.selected.toList() }

    fun exportRest(destination: Uri) = runExport(destination) { state ->
        state.pages.indices.filterNot { it in state.selected }
    }

    private fun runExport(destination: Uri, pagesToKeep: (SplitUiState) -> List<Int>) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        val indices = pagesToKeep(state)
        if (indices.isEmpty()) {
            _uiState.update { it.copy(status = SplitStatus.NEED_ONE) }
            return
        }
        _uiState.update { it.copy(status = SplitStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = PdfOperations.extractPages(context, source, destination, indices)
            _uiState.update {
                it.copy(status = if (result.isSuccess) SplitStatus.SUCCESS else SplitStatus.ERROR)
            }
        }
    }
}
