package com.pdfsuite.app.ui.viewer

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfRendererHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewerUiState(
    val isLoading: Boolean = false,
    val pages: List<Bitmap> = emptyList(),
    val error: Boolean = false,
)

class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun openPdf(uri: Uri, targetWidthPx: Int) {
        _uiState.value = ViewerUiState(isLoading = true)
        viewModelScope.launch {
            val context = getApplication<Application>()
            runCatching {
                PdfRendererHelper.renderAllPages(context, uri, targetWidthPx)
            }.onSuccess { pages ->
                _uiState.value = ViewerUiState(isLoading = false, pages = pages)
            }.onFailure {
                _uiState.value = ViewerUiState(isLoading = false, error = true)
            }
        }
    }
}
