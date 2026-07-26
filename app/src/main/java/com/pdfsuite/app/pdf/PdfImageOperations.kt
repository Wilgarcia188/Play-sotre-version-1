package com.pdfsuite.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_PAGE_WIDTH_PT = 612f
private const val DEFAULT_PAGE_HEIGHT_PT = 792f

/**
 * Image-based PDF operations backed by PdfBox-Android: compressing a PDF by
 * rasterizing each page and re-encoding it as JPEG, and building a PDF from
 * a list of images. This trades any real (selectable) text layer for a much
 * smaller/simpler file - the right tradeoff for scanned/photographed pages,
 * which is the common case for both of these operations.
 */
object PdfImageOperations {

    suspend fun compressPdf(
        context: Context,
        source: Uri,
        destination: Uri,
        quality: Float,
        maxPageWidthPx: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val pageCount = PdfRendererHelper.getPageCount(context, source)
            require(pageCount > 0) { "Empty PDF" }

            val pageSizes = resolver.openInputStream(source)?.use { input ->
                PDDocument.load(input).use { doc ->
                    (0 until doc.numberOfPages).map { index ->
                        val box = doc.getPage(index).mediaBox
                        box.width to box.height
                    }
                }
            } ?: error("Could not open $source")

            PDDocument().use { newDoc ->
                for (pageIndex in 0 until pageCount) {
                    val (widthPt, heightPt) = pageSizes.getOrElse(pageIndex) {
                        DEFAULT_PAGE_WIDTH_PT to DEFAULT_PAGE_HEIGHT_PT
                    }
                    val bitmap = PdfRendererHelper.renderPage(context, source, pageIndex, maxPageWidthPx)
                        ?: continue
                    val image = try {
                        JPEGFactory.createFromImage(newDoc, bitmap, quality)
                    } finally {
                        bitmap.recycle()
                    }

                    val page = PDPage(PDRectangle(widthPt, heightPt))
                    newDoc.addPage(page)
                    PDPageContentStream(newDoc, page).use { stream ->
                        stream.drawImage(image, 0f, 0f, widthPt, heightPt)
                    }
                }
                resolver.openOutputStream(destination)?.use { output ->
                    newDoc.save(output)
                } ?: error("Could not open destination $destination")
            }
        }
    }

    suspend fun imagesToPdf(
        context: Context,
        images: List<Uri>,
        destination: Uri,
        quality: Float = 0.85f,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(images.isNotEmpty()) { "No images selected" }
            val resolver = context.contentResolver

            PDDocument().use { doc ->
                images.forEach { uri ->
                    val bitmap = resolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    } ?: error("Could not decode $uri")

                    val image = try {
                        JPEGFactory.createFromImage(doc, bitmap, quality)
                    } finally {
                        bitmap.recycle()
                    }

                    // 1 image pixel = 1 PDF point: keeps the page's aspect ratio
                    // without needing to know the image's original DPI.
                    val widthPt = image.width.toFloat()
                    val heightPt = image.height.toFloat()
                    val page = PDPage(PDRectangle(widthPt, heightPt))
                    doc.addPage(page)
                    PDPageContentStream(doc, page).use { stream ->
                        stream.drawImage(image, 0f, 0f, widthPt, heightPt)
                    }
                }
                resolver.openOutputStream(destination)?.use { output ->
                    doc.save(output)
                } ?: error("Could not open destination $destination")
            }
        }
    }
}
