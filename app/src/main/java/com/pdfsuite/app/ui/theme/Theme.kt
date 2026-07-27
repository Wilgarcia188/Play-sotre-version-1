package com.pdfsuite.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PdfIndigo80,
    onPrimary = Color(0xFF241C55),
    primaryContainer = PdfIndigoContainerDark,
    onPrimaryContainer = PdfIndigo80,
    secondary = PdfIndigo80,
    background = PdfBackgroundDark,
    onBackground = PdfOnSurfaceDark,
    surface = PdfSurfaceDark,
    onSurface = PdfOnSurfaceDark,
    surfaceVariant = PdfSurfaceVariantDark,
    onSurfaceVariant = PdfOnSurfaceMutedDark,
)

private val LightColors = lightColorScheme(
    primary = PdfIndigo40,
    onPrimary = Color.White,
    primaryContainer = PdfIndigoContainerLight,
    onPrimaryContainer = PdfIndigo40,
    secondary = PdfIndigo40,
    background = PdfBackgroundLight,
    onBackground = PdfOnSurfaceLight,
    surface = PdfSurfaceLight,
    onSurface = PdfOnSurfaceLight,
    surfaceVariant = PdfIndigoContainerLight,
    onSurfaceVariant = PdfOnSurfaceMutedLight,
)

/**
 * [dynamicColor] defaults to false: on Android 12+ dynamic color derives the
 * palette from the user's wallpaper, which can wash out this app's deliberate
 * indigo-on-near-black look into a muddy, low-contrast scheme.
 */
@Composable
fun PdfSuiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PdfSuiteTypography,
        content = content,
    )
}
