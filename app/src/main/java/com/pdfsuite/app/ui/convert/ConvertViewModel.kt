package com.pdfsuite.app.ui.convert

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsuite.app.pdf.PdfImageOperations
import com.pdfsuite.app.pdf.PdfRendererHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImagesToPdfStatus { IDLE, WORKING, SUCCESS, ERROR, NEED_IMAGE }

data class ImagesToPdfUiState(
    val images: List<Uri> = emptyList(),
    val status: ImagesToPdfStatus = ImagesToPdfStatus.IDLE,
)

enum class PdfToImagesStatus { IDLE, READY, WORKING, SUCCESS, ERROR }

data class PdfToImagesUiState(
    val sourceUri: Uri? = null,
    val totalPages: Int = 0,
    val processedPages: Int = 0,
    val status: PdfToImagesStatus = PdfToImagesStatus.IDLE,
)

private const val EXPORT_PAGE_WIDTH_PX = 1600
private const val EXPORT_JPEG_QUALITY = 90

class ConvertViewModel(application: Application) : AndroidViewModel(application) {

    private val _imagesToPdfState = MutableStateFlow(ImagesToPdfUiState())
    val imagesToPdfState: StateFlow<ImagesToPdfUiState> = _imagesToPdfState.asStateFlow()

    private val _pdfToImagesState = MutableStateFlow(PdfToImagesUiState())
    val pdfToImagesState: StateFlow<PdfToImagesUiState> = _pdfToImagesState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        _imagesToPdfState.update {
            it.copy(images = it.images + uris, status = ImagesToPdfStatus.IDLE)
        }
    }

    fun removeImage(index: Int) {
        _imagesToPdfState.update { state ->
            state.copy(images = state.images.toMutableList().apply { removeAt(index) })
        }
    }

    fun moveImageUp(index: Int) = reorderImages(index, index - 1)

    fun moveImageDown(index: Int) = reorderImages(index, index + 1)

    private fun reorderImages(from: Int, to: Int) {
        _imagesToPdfState.update { state ->
            if (to < 0 || to >= state.images.size) return@update state
            val updated = state.images.toMutableList()
            val item = updated.removeAt(from)
            updated.add(to, item)
            state.copy(images = updated)
        }
    }

    fun createPdf(destination: Uri) {
        val images = _imagesToPdfState.value.images
        if (images.isEmpty()) {
            _imagesToPdfState.update { it.copy(status = ImagesToPdfStatus.NEED_IMAGE) }
            return
        }
        _imagesToPdfState.update { it.copy(status = ImagesToPdfStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = PdfImageOperations.imagesToPdf(context, images, destination)
            _imagesToPdfState.update {
                it.copy(status = if (result.isSuccess) ImagesToPdfStatus.SUCCESS else ImagesToPdfStatus.ERROR)
            }
        }
    }

    fun selectPdf(uri: Uri) {
        _pdfToImagesState.value = PdfToImagesUiState(sourceUri = uri, status = PdfToImagesStatus.READY)
    }

    fun exportPagesAsImages(folderUri: Uri) {
        val source = _pdfToImagesState.value.sourceUri ?: return
        _pdfToImagesState.update { it.copy(status = PdfToImagesStatus.WORKING) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val folder = DocumentFile.fromTreeUri(context, folderUri)
                        ?: error("Could not open folder $folderUri")
                    val pageCount = PdfRendererHelper.getPageCount(context, source)
                    _pdfToImagesState.update { it.copy(totalPages = pageCount) }

                    for (pageIndex in 0 until pageCount) {
                        val bitmap = PdfRendererHelper.renderPage(context, source, pageIndex, EXPORT_PAGE_WIDTH_PX)
                            ?: continue
                        writePageImage(context, folder, bitmap, pageIndex)
                        _pdfToImagesState.update { it.copy(processedPages = pageIndex + 1) }
                    }
                }
            }
            _pdfToImagesState.update {
                it.copy(status = if (result.isSuccess) PdfToImagesStatus.SUCCESS else PdfToImagesStatus.ERROR)
            }
        }
    }

    private fun writePageImage(
        context: android.content.Context,
        folder: DocumentFile,
        bitmap: Bitmap,
        pageIndex: Int,
    ) {
        try {
            val fileName = "pagina_%03d.jpg".format(pageIndex + 1)
            val file = folder.createFile("image/jpeg", fileName) ?: error("Could not create $fileName")
            context.contentResolver.openOutputStream(file.uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, EXPORT_JPEG_QUALITY, output)
            } ?: error("Could not open output stream for $fileName")
        } finally {
            bitmap.recycle()
        }
    }
}
