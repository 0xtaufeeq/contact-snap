package com.contactsnap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A frosted "liquid glass" surface: a translucent tint (so content shows through),
 * a soft top-down gloss sheen, and a light edge highlight.
 */
@Composable
fun Modifier.glass(shape: Shape, tintAlpha: Float = 0.62f): Modifier {
    val tint = MaterialTheme.colorScheme.surfaceVariant
    return this
        .clip(shape)
        .background(tint.copy(alpha = tintAlpha))
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.0f))
            )
        )
        .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
}
