package com.pdfsuite.app.ui.merge

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MergeStatus { IDLE, WORKING, SUCCESS, ERROR, NEED_TWO }

data class MergeUiState(
    val files: List<Uri> = emptyList(),
    val status: MergeStatus = MergeStatus.IDLE,
)

class MergeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        _uiState.update { it.copy(files = it.files + uris, status = MergeStatus.IDLE) }
    }

    fun moveUp(index: Int) = reorder(index, index - 1)

    fun moveDown(index: Int) = reorder(index, index + 1)

    fun remove(index: Int) {
        _uiState.update { state ->
            state.copy(files = state.files.toMutableList().apply { removeAt(index) })
        }
    }

    private fun reorder(from: Int, to: Int) {
        _uiState.update { state ->
            if (to < 0 || to >= state.files.size) return@update state
            val updated = state.files.toMutableList()
            val item = updated.removeAt(from)
            updated.add(to, item)
            state.copy(files = updated)
        }
    }

    fun merge(destination: Uri) {
        val files = _uiState.value.files
        if (files.size < 2) {
            _uiState.update { it.copy(status = MergeStatus.NEED_TWO) }
            return
        }
        _uiState.update { it.copy(status = MergeStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = PdfOperations.mergePdfs(context, files, destination)
            _uiState.update {
                it.copy(status = if (result.isSuccess) MergeStatus.SUCCESS else MergeStatus.ERROR)
            }
        }
    }
}
