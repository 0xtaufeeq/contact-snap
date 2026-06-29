package com.contactsnap.app.util

import androidx.compose.ui.graphics.Color

/** Palette + stable default color for group labels. */
object GroupColors {

    // Muted, editorial tones that read well on the paper/ink theme.
    val palette: List<Color> = listOf(
        Color(0xFFB6552F), // terracotta
        Color(0xFF4E6E58), // sage
        Color(0xFF3D5A80), // slate blue
        Color(0xFF8A5A83), // plum
        Color(0xFFB08900), // ochre
        Color(0xFF9A958C), // stone
        Color(0xFF2A9D8F), // teal
        Color(0xFFC1666B), // dusty rose
    )

    /** A consistent color derived from the name, used when none is set. */
    fun default(name: String): Color {
        if (name.isBlank()) return palette[0]
        val idx = (name.hashCode() % palette.size + palette.size) % palette.size
        return palette[idx]
    }

    /** Effective color: an explicit override (ARGB int) or the derived default. */
    fun effective(name: String, override: Int?): Color =
        override?.let { Color(it) } ?: default(name)
}
