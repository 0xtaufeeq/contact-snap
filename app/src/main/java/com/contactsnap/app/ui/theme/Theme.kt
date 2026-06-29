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

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    tertiary = AccentDark,
    background = BgDark,
    onBackground = TextDark,
    surface = BgDark,
    onSurface = TextDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineDimDark,
    surfaceContainerLowest = BgDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardHighDark
)

private val AmoledColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    tertiary = AccentDark,
    background = BgAmoled,
    onBackground = TextDark,
    surface = BgAmoled,
    onSurface = TextDark,
    surfaceVariant = CardAmoled,
    onSurfaceVariant = TextMutedDark,
    outline = OutlineAmoled,
    outlineVariant = OutlineDimAmoled,
    surfaceContainerLowest = BgAmoled,
    surfaceContainer = CardAmoled,
    surfaceContainerHigh = CardHighAmoled
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    secondary = AccentLight,
    onSecondary = OnAccentLight,
    tertiary = AccentLight,
    background = BgLight,
    onBackground = TextLight,
    surface = BgLight,
    onSurface = TextLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextMutedLight,
    outline = OutlineLight,
    outlineVariant = OutlineDimLight,
    surfaceContainerLowest = BgLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = CardHighLight
)

@Composable
fun ContactSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        amoled -> AmoledColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !(darkTheme || amoled)
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
