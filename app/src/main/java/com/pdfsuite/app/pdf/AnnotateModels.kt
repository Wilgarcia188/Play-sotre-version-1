package com.pdfsuite.app.pdf

import android.graphics.Bitmap

enum class AnnotationTool { PEN, HIGHLIGHTER, TEXT, SIGNATURE }

/** A point as a fraction (0f..1f) of the page's width/height, independent of preview resolution. */
data class NormalizedPoint(val x: Float, val y: Float)

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
}
