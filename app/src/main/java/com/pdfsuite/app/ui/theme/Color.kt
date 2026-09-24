package com.pdfsuite.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand accent: indigo-violet, used for the primary action and the app identity.
val PdfIndigoLight = Color(0xFF5B4FE0)
val PdfIndigoDark = Color(0xFFB4ADFF)

// Secondary accent, used for gradients and highlights alongside the indigo.
val PdfVioletLight = Color(0xFF9333EA)
val PdfVioletDark = Color(0xFFE0B6FF)

// Dark surfaces, layered from the background up so cards separate without borders.
val PdfBackgroundDark = Color(0xFF0B0B11)
val PdfSurfaceDark = Color(0xFF13131C)
val PdfSurfaceContainerDark = Color(0xFF1A1A25)
val PdfSurfaceContainerHighDark = Color(0xFF232331)
val PdfIndigoContainerDark = Color(0xFF2E2A57)
val PdfOnSurfaceDark = Color(0xFFECEBF4)
val PdfOnSurfaceMutedDark = Color(0xFF9E9FB4)
val PdfOutlineDark = Color(0xFF32333F)

// Light surfaces, same layering on a soft off-white base.
val PdfBackgroundLight = Color(0xFFF7F6FC)
val PdfSurfaceLight = Color(0xFFFFFFFF)
val PdfSurfaceContainerLight = Color(0xFFF1F0F8)
val PdfSurfaceContainerHighLight = Color(0xFFE9E8F3)
val PdfIndigoContainerLight = Color(0xFFE6E3FF)
val PdfOnSurfaceLight = Color(0xFF16161F)
val PdfOnSurfaceMutedLight = Color(0xFF5A5C6C)
val PdfOutlineLight = Color(0xFFE1DFEE)

val PdfErrorLight = Color(0xFFBA1A1A)
val PdfErrorDark = Color(0xFFFFB4AB)

/** Per-tool accent, so the home grid reads as distinct tools instead of ten identical rows. */
enum class ToolAccent(val light: Color, val dark: Color) {
    INDIGO(Color(0xFF5B4FE0), Color(0xFFB4ADFF)),
    VIOLET(Color(0xFF9333EA), Color(0xFFDDA8FF)),
    SKY(Color(0xFF0B79D0), Color(0xFF8FCBFF)),
    TEAL(Color(0xFF00897B), Color(0xFF6FDCCB)),
    AMBER(Color(0xFFB26A00), Color(0xFFFFC46B)),
    CORAL(Color(0xFFD1435B), Color(0xFFFF9FAC)),
}
