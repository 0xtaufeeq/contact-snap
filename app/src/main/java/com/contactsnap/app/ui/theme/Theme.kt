package com.contactsnap.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Accent,
    onSecondary = Paper,
    tertiary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = Hairline,
    surfaceContainer = PaperDim,
    surfaceContainerHigh = AccentSoft
)

private val DarkColors = darkColorScheme(
    primary = PaperInk,
    onPrimary = InkPaper,
    secondary = AccentDark,
    onSecondary = InkPaper,
    tertiary = AccentDark,
    background = InkPaper,
    onBackground = PaperInk,
    surface = InkPaper,
    onSurface = PaperInk,
    surfaceVariant = InkPaperDim,
    onSurfaceVariant = PaperInkSoft,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    surfaceContainer = InkPaperDim,
    surfaceContainerHigh = HairlineDark
)

@Composable
fun ContactSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
