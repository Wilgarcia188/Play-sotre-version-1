package com.pdfsuite.app.ui.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanStatus { IDLE, SCANNED, SAVING, SUCCESS, ERROR }

data class ScanUiState(
    val scannedPdfUri: Uri? = null,
    val pageCount: Int = 0,
    val status: ScanStatus = ScanStatus.IDLE,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onScanResult(pdfUri: Uri?, pageCount: Int) {
        _uiState.value = if (pdfUri != null) {
            ScanUiState(scannedPdfUri = pdfUri, pageCount = pageCount, status = ScanStatus.SCANNED)
        } else {
            ScanUiState()
        }
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }

    fun save(destination: Uri) {
        val source = _uiState.value.scannedPdfUri ?: return
        _uiState.update { it.copy(status = ScanStatus.SAVING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    resolver.openInputStream(source)?.use { input ->
                        resolver.openOutputStream(destination)?.use { output ->
                            input.copyTo(output)
                        } ?: error("Could not open destination $destination")
                    } ?: error("Could not open scanned file $source")
                }
            }
            _uiState.update {
                it.copy(status = if (result.isSuccess) ScanStatus.SUCCESS else ScanStatus.ERROR)
            }
        }
    }
}
