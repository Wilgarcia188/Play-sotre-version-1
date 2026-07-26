package com.pdfsuite.app.ui.compress

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfImageOperations
import com.pdfsuite.app.pdf.fileSizeBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CompressionLevel(val quality: Float, val maxPageWidthPx: Int) {
    HIGH(quality = 0.8f, maxPageWidthPx = 2000),
    BALANCED(quality = 0.55f, maxPageWidthPx = 1500),
    SMALL(quality = 0.35f, maxPageWidthPx = 1100),
}

enum class CompressStatus { IDLE, READY, WORKING, DONE, ERROR }

data class CompressUiState(
    val sourceUri: Uri? = null,
    val originalSizeBytes: Long? = null,
    val compressedSizeBytes: Long? = null,
    val level: CompressionLevel = CompressionLevel.BALANCED,
    val status: CompressStatus = CompressStatus.IDLE,
)

class CompressViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CompressUiState())
    val uiState: StateFlow<CompressUiState> = _uiState.asStateFlow()

    fun selectFile(uri: Uri) {
        val context = getApplication<Application>()
        _uiState.value = CompressUiState(
            sourceUri = uri,
            originalSizeBytes = uri.fileSizeBytes(context),
            status = CompressStatus.READY,
        )
    }

    fun setLevel(level: CompressionLevel) {
        _uiState.update { it.copy(level = level) }
    }

    fun compress(destination: Uri) {
        val source = _uiState.value.sourceUri ?: return
        val level = _uiState.value.level
        _uiState.update { it.copy(status = CompressStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = PdfImageOperations.compressPdf(
                context = context,
                source = source,
                destination = destination,
                quality = level.quality,
                maxPageWidthPx = level.maxPageWidthPx,
            )
            val compressedSize = if (result.isSuccess) destination.fileSizeBytes(context) else null
            _uiState.update {
                it.copy(
                    status = if (result.isSuccess) CompressStatus.DONE else CompressStatus.ERROR,
                    compressedSizeBytes = compressedSize,
                )
            }
        }
    }
}
