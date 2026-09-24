package com.pdfsuite.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.pdfsuite.app.pdf.findActivity

private val DarkColors = darkColorScheme(
    primary = PdfIndigoDark,
    onPrimary = Color(0xFF221A52),
    primaryContainer = PdfIndigoContainerDark,
    onPrimaryContainer = PdfIndigoDark,
    secondary = PdfVioletDark,
    onSecondary = Color(0xFF3B0764),
    tertiary = PdfVioletDark,
    background = PdfBackgroundDark,
    onBackground = PdfOnSurfaceDark,
    surface = PdfSurfaceDark,
    onSurface = PdfOnSurfaceDark,
    surfaceVariant = PdfSurfaceContainerHighDark,
    onSurfaceVariant = PdfOnSurfaceMutedDark,
    surfaceContainer = PdfSurfaceContainerDark,
    surfaceContainerHigh = PdfSurfaceContainerHighDark,
    surfaceContainerHighest = PdfSurfaceContainerHighDark,
    outline = PdfOutlineDark,
    outlineVariant = PdfOutlineDark,
    error = PdfErrorDark,
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = PdfIndigoLight,
    onPrimary = Color.White,
    primaryContainer = PdfIndigoContainerLight,
    onPrimaryContainer = PdfIndigoLight,
    secondary = PdfVioletLight,
    onSecondary = Color.White,
    tertiary = PdfVioletLight,
    background = PdfBackgroundLight,
    onBackground = PdfOnSurfaceLight,
    surface = PdfSurfaceLight,
    onSurface = PdfOnSurfaceLight,
    surfaceVariant = PdfSurfaceContainerHighLight,
    onSurfaceVariant = PdfOnSurfaceMutedLight,
    surfaceContainer = PdfSurfaceContainerLight,
    surfaceContainerHigh = PdfSurfaceContainerHighLight,
    surfaceContainerHighest = PdfSurfaceContainerHighLight,
    outline = PdfOutlineLight,
    outlineVariant = PdfOutlineLight,
    error = PdfErrorLight,
    onError = Color.White,
)

private val PdfSuiteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
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
    val view = LocalView.current
    val context = LocalContext.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            // Status/navigation bar icons have to follow the app's own theme, not the
            // system's, since the app never switches to the wallpaper-derived palette.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PdfSuiteTypography,
        shapes = PdfSuiteShapes,
        content = content,
    )
}
