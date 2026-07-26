package com.pdfsuite.app.pdf

import android.graphics.Bitmap

enum class AnnotationTool { PEN, HIGHLIGHTER, TEXT, SIGNATURE, EDIT_TEXT }

/** Extra space below a [PdfTextChunk]'s baseline, as a fraction of its height,
 * so covering/rendering it also catches descenders (e.g. "g", "y", "p").
 * Shared between the on-screen preview and the actual saved PDF so they match. */
const val TEXT_REPLACEMENT_DESCENT_PADDING_FRACTION = 0.22f

/** Extra space above a [PdfTextChunk]'s estimated ascent, as a fraction of its
 * height, since [PdfTextChunk.height] can slightly undershoot tall/accented
 * characters not present in a given text run. */
const val TEXT_REPLACEMENT_TOP_PADDING_FRACTION = 0.12f

/** A point as a fraction (0f..1f) of the page's width/height, independent of preview resolution. */
data class NormalizedPoint(val x: Float, val y: Float)

/**
 * A run of existing text detected on a page, in PDF point space (the same
 * bottom-left-origin, y-up space [PDPageContentStream] draws in) so it can be
 * covered and redrawn without any further coordinate conversion.
 * [baselineY] is where the text's baseline sits; the run's ink extends from
 * roughly `baselineY - descent` up to `baselineY + ascent` (approximated here
 * as a fixed fraction of [height], since exact per-glyph metrics aren't
 * available from a text run).
 */
data class PdfTextChunk(
    val text: String,
    val x: Float,
    val baselineY: Float,
    val width: Float,
    val height: Float,
    val fontSizePt: Float,
)

/** Detected text runs for one page, plus the page's own size - needed to convert
 * on-screen tap fractions into this same PDF point space. */
data class PageTextInfo(
    val pageWidthPt: Float,
    val pageHeightPt: Float,
    val chunks: List<PdfTextChunk>,
)

sealed class PageAnnotation {
    data class Stroke(val points: List<NormalizedPoint>, val tool: AnnotationTool) : PageAnnotation()

    data class TextNote(val position: NormalizedPoint, val text: String) : PageAnnotation()

    /** [position] is the stamp's top-left corner, matching how it's drawn in the on-screen overlay. */
    data class Signature(
        val position: NormalizedPoint,
        val bitmap: Bitmap,
        val widthFraction: Float,
        val heightFraction: Float,
    ) : PageAnnotation()

    /** Covers [original]'s bounding box and draws [newText] in its place, same position/size. */
    data class TextReplacement(val original: PdfTextChunk, val newText: String) : PageAnnotation()
}
