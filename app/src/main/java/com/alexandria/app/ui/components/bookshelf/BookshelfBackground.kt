package com.alexandria.app.ui.components.bookshelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alexandria.app.domain.model.VisualMode

private val ShelfLineColor = Color(0xFF8D6E63)
private val RowHeightEstimate = 420.dp

fun Modifier.shelfGridBackground(visualMode: VisualMode): Modifier {
    if (visualMode != VisualMode.IMMERSIVE) return this
    val lineThickness = 3.dp
    val shadowHeight = 2.dp
    return this.drawBehind {
        val rowHeightPx = RowHeightEstimate.toPx()
        val linePx = lineThickness.toPx()
        val shadowPx = shadowHeight.toPx()
        var y = rowHeightPx

        while (y < size.height) {
            drawRect(
                color = ShelfLineColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, y - linePx / 2f),
                size = Size(size.width, linePx)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.08f),
                topLeft = Offset(0f, y + linePx / 2f),
                size = Size(size.width, shadowPx)
            )
            y += rowHeightPx
        }
    }
}

@Composable
fun BookshelfBackgroundContent(
    visualMode: VisualMode,
    content: @Composable () -> Unit
) {
    if (visualMode == VisualMode.IMMERSIVE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shelfGridBackground(visualMode)
        ) {
            content()
        }
    } else {
        content()
    }
}
