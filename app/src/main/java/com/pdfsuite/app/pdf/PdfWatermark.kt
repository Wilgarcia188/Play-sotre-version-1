package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WATERMARK_FONT_SIZE = 40f
private const val WATERMARK_ANGLE_DEGREES = 45.0
private const val WATERMARK_GRAY = 150

/**
 * Stamps a diagonal, semi-transparent text watermark on every page of a PDF.
 * Unlike the compress/convert operations, this appends to the existing page
 * content stream instead of rasterizing, so the original page content
 * (including any real, selectable text) is preserved as-is.
 */
object PdfWatermark {

    suspend fun addWatermark(
        context: Context,
        source: Uri,
        destination: Uri,
        text: String,
        opacity: Float,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(text.isNotBlank()) { "Watermark text is empty" }
            val resolver = context.contentResolver
            val font = PDType1Font.HELVETICA_BOLD
            val angleRadians = Math.toRadians(WATERMARK_ANGLE_DEGREES)
            val textWidth = font.getStringWidth(text) / 1000f * WATERMARK_FONT_SIZE

            resolver.openInputStream(source)?.use { input ->
                PDDocument.load(input).use { doc ->
                    val graphicsState = PDExtendedGraphicsState().apply {
                        setNonStrokingAlphaConstant(opacity)
                    }

                    for (pageIndex in 0 until doc.numberOfPages) {
                        val page = doc.getPage(pageIndex)
                        val box = page.mediaBox
                        val centerX = box.width / 2f
                        val centerY = box.height / 2f
                        val startX = (centerX - Math.cos(angleRadians) * textWidth / 2).toFloat()
                        val startY = (centerY - Math.sin(angleRadians) * textWidth / 2).toFloat()

                        // resetContext=true: an arbitrary page's existing content stream may leave the
                        // graphics state non-default (unbalanced q/clip, altered color), which would
                        // otherwise corrupt the watermark's position or color.
                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                            stream.saveGraphicsState()
                            stream.setGraphicsStateParameters(graphicsState)
                            stream.setNonStrokingColor(WATERMARK_GRAY, WATERMARK_GRAY, WATERMARK_GRAY)
                            stream.beginText()
                            stream.setFont(font, WATERMARK_FONT_SIZE)
                            stream.setTextMatrix(Matrix.getRotateInstance(angleRadians, startX, startY))
                            stream.showText(text)
                            stream.endText()
                            stream.restoreGraphicsState()
                        }
                    }

                    resolver.openOutputStream(destination)?.use { output ->
                        doc.save(output)
                    } ?: error("Could not open destination $destination")
                }
            } ?: error("Could not open $source")
        }
    }
}
