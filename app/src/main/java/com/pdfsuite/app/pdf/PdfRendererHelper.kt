package com.pdfsuite.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders PDF pages to bitmaps using the platform's built-in [PdfRenderer].
 * PdfRenderer only allows one open page at a time, so all rendering for a
 * given call happens sequentially against a single renderer instance.
 */
object PdfRendererHelper {

    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        withRenderer(context, uri) { renderer -> renderer.pageCount } ?: 0
    }

    /** Renders every page of [uri] to a bitmap whose width matches [targetWidthPx]. */
    suspend fun renderAllPages(
        context: Context,
        uri: Uri,
        targetWidthPx: Int,
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        withRenderer(context, uri) { renderer ->
            (0 until renderer.pageCount).map { index -> renderPageAt(renderer, index, targetWidthPx) }
        } ?: emptyList()
    }

    suspend fun renderPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidthPx: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        withRenderer(context, uri) { renderer ->
            if (pageIndex in 0 until renderer.pageCount) {
                renderPageAt(renderer, pageIndex, targetWidthPx)
            } else {
                null
            }
        }
    }

    private fun renderPageAt(renderer: PdfRenderer, index: Int, targetWidthPx: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width
            val width = targetWidthPx.coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    /** Opens [uri] as a [PdfRenderer], guaranteeing both the renderer and its
     * underlying file descriptor are closed even if [block] throws. */
    private inline fun <T> withRenderer(context: Context, uri: Uri, block: (PdfRenderer) -> T): T? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return pfd.use {
            PdfRenderer(it).use { renderer -> block(renderer) }
        }
    }
}
