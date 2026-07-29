package com.alexandria.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.theme.GenreColorMapper

fun Modifier.coverGradientScrim(genre: String?, visualMode: VisualMode): Modifier {
    if (visualMode != VisualMode.IMMERSIVE) return this
    val color = GenreColorMapper.colorFor(genre)
    return this.drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    color.copy(alpha = 0.15f),
                    color.copy(alpha = 0.35f)
                ),
                startY = size.height * 0.5f,
                endY = size.height
            ),
            size = size
        )
    }
}

fun Modifier.coverEdgeFade(genre: String?, visualMode: VisualMode): Modifier {
    if (visualMode != VisualMode.IMMERSIVE) return this
    val color = GenreColorMapper.colorFor(genre)
    return this.drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.6f),
                    Color.Transparent,
                    Color.Transparent,
                    color.copy(alpha = 0.3f)
                ),
                startY = 0f,
                endY = size.height
            ),
            size = size
        )
    }
}
