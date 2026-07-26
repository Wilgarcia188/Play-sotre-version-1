package com.pdfsuite.app.ui.watermark

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfWatermark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WatermarkOpacity(val alpha: Float) {
    SUBTLE(0.15f),
    MEDIUM(0.3f),
    STRONG(0.5f),
}

enum class WatermarkStatus { IDLE, READY, WORKING, SUCCESS, ERROR, NEED_TEXT }

data class WatermarkUiState(
    val sourceUri: Uri? = null,
    val text: String = "",
    val opacity: WatermarkOpacity = WatermarkOpacity.MEDIUM,
    val status: WatermarkStatus = WatermarkStatus.IDLE,
)

class WatermarkViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WatermarkUiState())
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    fun selectFile(uri: Uri) {
        _uiState.update { it.copy(sourceUri = uri, status = WatermarkStatus.READY) }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun setOpacity(opacity: WatermarkOpacity) {
        _uiState.update { it.copy(opacity = opacity) }
    }

    fun applyWatermark(destination: Uri) {
        val state = _uiState.value
        val source = state.sourceUri ?: return
        if (state.text.isBlank()) {
            _uiState.update { it.copy(status = WatermarkStatus.NEED_TEXT) }
            return
        }
        _uiState.update { it.copy(status = WatermarkStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = PdfWatermark.addWatermark(
                context = context,
                source = source,
                destination = destination,
                text = state.text,
                opacity = state.opacity.alpha,
            )
            _uiState.update {
                it.copy(status = if (result.isSuccess) WatermarkStatus.SUCCESS else WatermarkStatus.ERROR)
            }
        }
    }
}
