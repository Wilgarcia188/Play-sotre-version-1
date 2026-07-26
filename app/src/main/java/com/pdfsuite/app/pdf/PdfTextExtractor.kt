package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Locates existing text runs on a page (position + size in PDF points) so the
 * "Editar texto" tool can let the user tap on real document text and replace
 * it in place, instead of only adding new overlay annotations.
 */
object PdfTextExtractor {

    suspend fun extractPageText(context: Context, uri: Uri, pageIndex: Int): PageTextInfo? =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { doc ->
                    if (pageIndex !in 0 until doc.numberOfPages) return@withContext null
                    val page = doc.getPage(pageIndex)
                    val chunks = mutableListOf<PdfTextChunk>()

                    val pageHeightPt = page.mediaBox.height
                    val stripper = object : PDFTextStripper() {
                        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                            if (text.isNotBlank() && textPositions.isNotEmpty()) {
                                chunks += toChunk(text, textPositions, pageHeightPt)
                            }
                        }
                    }
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    stripper.getText(doc)

                    PageTextInfo(
                        pageWidthPt = page.mediaBox.width,
                        pageHeightPt = pageHeightPt,
                        chunks = chunks,
                    )
                }
            }
        }

    private fun toChunk(text: String, positions: List<TextPosition>, pageHeightPt: Float): PdfTextChunk {
        // TextPosition's x/y are in a top-down (y grows downward from the page's
        // top) reading space - the opposite of PDPageContentStream's bottom-up
        // drawing space - so the baseline is flipped back here once, up front,
        // to keep every downstream consumer of PdfTextChunk in one coordinate system.
        val left = positions.minOf { it.xDirAdj }
        val right = positions.maxOf { it.xDirAdj + it.widthDirAdj }
        val baselineYTopDown = positions.first().yDirAdj
        val height = positions.maxOf { it.heightDir }
        return PdfTextChunk(
            text = text,
            x = left,
            baselineY = pageHeightPt - baselineYTopDown,
            width = right - left,
            height = height,
            fontSizePt = positions.first().fontSizeInPt,
        )
    }
}
