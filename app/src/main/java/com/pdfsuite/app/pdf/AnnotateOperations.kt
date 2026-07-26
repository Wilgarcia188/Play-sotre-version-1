package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PEN_WIDTH_PT = 3f
private const val HIGHLIGHTER_WIDTH_PT = 16f
private const val HIGHLIGHTER_ALPHA = 0.4f
private const val TEXT_FONT_SIZE_PT = 16f

// PDF line cap/join style codes (PDF spec: 0=butt/miter, 1=round, 2=projecting-square/bevel).
private const val LINE_CAP_ROUND = 1
private const val LINE_JOIN_ROUND = 1

/**
 * Burns freehand strokes, text notes and signature stamps into a PDF by
 * appending to each page's existing content stream (not rasterizing), the
 * same technique used by [PdfWatermark].
 */
object AnnotateOperations {

    suspend fun applyAnnotations(
        context: Context,
        source: Uri,
        destination: Uri,
        annotationsByPage: Map<Int, List<PageAnnotation>>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            resolver.openInputStream(source)?.use { input ->
                PDDocument.load(input).use { doc ->
                    annotationsByPage.forEach { (pageIndex, annotations) ->
                        if (annotations.isEmpty() || pageIndex !in 0 until doc.numberOfPages) return@forEach
                        val page = doc.getPage(pageIndex)
                        val pageWidth = page.mediaBox.width
                        val pageHeight = page.mediaBox.height

                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                            annotations.forEach { annotation ->
                                when (annotation) {
                                    is PageAnnotation.Stroke -> drawStroke(stream, annotation, pageWidth, pageHeight)
                                    is PageAnnotation.TextNote -> drawText(stream, annotation, pageWidth, pageHeight)
                                    is PageAnnotation.Signature -> drawSignature(doc, stream, annotation, pageWidth, pageHeight)
                                    is PageAnnotation.TextReplacement -> drawTextReplacement(stream, annotation)
                                }
                            }
                        }
                    }
                    resolver.openOutputStream(destination)?.use { output ->
                        doc.save(output)
                    } ?: error("Could not open destination $destination")
                }
            } ?: error("Could not open $source")
        }
    }

    private fun drawStroke(
        stream: PDPageContentStream,
        stroke: PageAnnotation.Stroke,
        pageWidth: Float,
        pageHeight: Float,
    ) {
        if (stroke.points.size < 2) return
        val isHighlighter = stroke.tool == AnnotationTool.HIGHLIGHTER

        stream.saveGraphicsState()
        if (isHighlighter) {
            val graphicsState = PDExtendedGraphicsState().apply { setStrokingAlphaConstant(HIGHLIGHTER_ALPHA) }
            stream.setGraphicsStateParameters(graphicsState)
            stream.setStrokingColor(255, 235, 59)
            stream.setLineWidth(HIGHLIGHTER_WIDTH_PT)
        } else {
            stream.setStrokingColor(220, 0, 0)
            stream.setLineWidth(PEN_WIDTH_PT)
        }
        stream.setLineCapStyle(LINE_CAP_ROUND)
        stream.setLineJoinStyle(LINE_JOIN_ROUND)

        val first = stroke.points.first()
        stream.moveTo(first.x * pageWidth, pageHeight - first.y * pageHeight)
        stroke.points.drop(1).forEach { point ->
            stream.lineTo(point.x * pageWidth, pageHeight - point.y * pageHeight)
        }
        stream.stroke()
        stream.restoreGraphicsState()
    }

    private fun drawText(
        stream: PDPageContentStream,
        note: PageAnnotation.TextNote,
        pageWidth: Float,
        pageHeight: Float,
    ) {
        if (note.text.isBlank()) return
        stream.saveGraphicsState()
        stream.setNonStrokingColor(0, 0, 0)
        stream.beginText()
        stream.setFont(PDType1Font.HELVETICA_BOLD, TEXT_FONT_SIZE_PT)
        stream.newLineAtOffset(note.position.x * pageWidth, pageHeight - note.position.y * pageHeight)
        stream.showText(note.text)
        stream.endText()
        stream.restoreGraphicsState()
    }

    private fun drawSignature(
        doc: PDDocument,
        stream: PDPageContentStream,
        signature: PageAnnotation.Signature,
        pageWidth: Float,
        pageHeight: Float,
    ) {
        val image = LosslessFactory.createFromImage(doc, signature.bitmap)
        val widthPt = signature.widthFraction * pageWidth
        val heightPt = signature.heightFraction * pageHeight
        val x = signature.position.x * pageWidth
        val y = pageHeight - signature.position.y * pageHeight - heightPt
        stream.drawImage(image, x, y, widthPt, heightPt)
    }

    /** [PdfTextChunk] is already in absolute PDF point space, so no page width/height scaling is needed here. */
    private fun drawTextReplacement(stream: PDPageContentStream, replacement: PageAnnotation.TextReplacement) {
        val chunk = replacement.original
        val descentPadding = chunk.height * TEXT_REPLACEMENT_DESCENT_PADDING_FRACTION
        // A little extra above the estimated ascent too: [PdfTextChunk.height] is derived from
        // one run's glyph metrics and can slightly undershoot tall/accented characters.
        val topPadding = chunk.height * TEXT_REPLACEMENT_TOP_PADDING_FRACTION

        stream.saveGraphicsState()
        stream.setNonStrokingColor(255, 255, 255)
        stream.fillRect(
            chunk.x,
            chunk.baselineY - descentPadding,
            chunk.width,
            chunk.height + descentPadding + topPadding,
        )
        stream.restoreGraphicsState()

        if (replacement.newText.isNotBlank()) {
            stream.saveGraphicsState()
            stream.setNonStrokingColor(0, 0, 0)
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, chunk.fontSizePt)
            stream.newLineAtOffset(chunk.x, chunk.baselineY)
            stream.showText(replacement.newText)
            stream.endText()
            stream.restoreGraphicsState()
        }
    }
}
