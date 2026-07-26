package com.pdfsuite.app.ui.ocr

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.ocr.OcrHelper
import com.pdfsuite.app.pdf.PdfRendererHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OcrStatus { IDLE, PROCESSING, DONE, ERROR, EXPORTED, EXPORT_ERROR }

data class OcrUiState(
    val status: OcrStatus = OcrStatus.IDLE,
    val totalPages: Int = 0,
    val processedPages: Int = 0,
    val recognizedText: String = "",
)

private const val OCR_PAGE_WIDTH_PX = 1200

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun startOcr(uri: Uri) {
        _uiState.value = OcrUiState(status = OcrStatus.PROCESSING)
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = runCatching {
                val pageCount = PdfRendererHelper.getPageCount(context, uri)
                _uiState.update { it.copy(totalPages = pageCount) }

                val text = StringBuilder()
                for (pageIndex in 0 until pageCount) {
                    val bitmap = PdfRendererHelper.renderPage(context, uri, pageIndex, OCR_PAGE_WIDTH_PX)
                    val pageText = bitmap?.let {
                        try {
                            OcrHelper.recognizeText(it)
                        } finally {
                            it.recycle()
                        }
                    }.orEmpty()

                    if (pageText.isNotBlank()) {
                        if (text.isNotEmpty()) text.append("\n\n")
                        text.append("--- Página ${pageIndex + 1} ---\n\n").append(pageText)
                    }
                    _uiState.update {
                        it.copy(processedPages = pageIndex + 1, recognizedText = text.toString())
                    }
                }
            }
            if (result.isFailure) {
                _uiState.update { it.copy(status = OcrStatus.ERROR) }
            } else {
                _uiState.update { it.copy(status = OcrStatus.DONE) }
            }
        }
    }

    fun exportText(destination: Uri) {
        val text = _uiState.value.recognizedText
        viewModelScope.launch {
            val context = getApplication<Application>()
            val success = runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    output.write(text.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open $destination")
            }.isSuccess
            _uiState.update { it.copy(status = if (success) OcrStatus.EXPORTED else OcrStatus.EXPORT_ERROR) }
        }
    }
}
