package com.alexandria.app.ui.components.bookshelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.theme.GenreColorMapper
import com.alexandria.app.ui.theme.LocalVisualMode

@Composable
fun BookshelfEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visualMode = LocalVisualMode.current
    if (visualMode != VisualMode.IMMERSIVE) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            drawBookshelf()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun DrawScope.drawBookshelf() {
    val shelfY = size.height * 0.75f
    val shelfColor = GenreColorMapper.shelfColor

    drawRect(
        color = shelfColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, shelfY),
        size = Size(size.width, 6.dp.toPx())
    )

    val bookColors = listOf(
        Color(0xFFC62828), Color(0xFF1565C0), Color(0xFF2E7D32),
        Color(0xFF6A1B9A), Color(0xFFE65100), Color(0xFF283593),
        Color(0xFF00838F), Color(0xFFAD1457)
    )

    val totalWidth = size.width * 0.8f
    val startX = size.width * 0.1f
    val bookSpacing = totalWidth / bookColors.size
    val bookHeight = shelfY - size.height * 0.15f

    bookColors.forEachIndexed { index, color ->
        val x = startX + index * bookSpacing
        val width = bookSpacing * 0.6f
        val tilt = (index % 3 - 1) * 0.03f

        drawRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(
                x + (bookSpacing - width) / 2f + tilt * bookHeight,
                size.height * 0.15f
            ),
            size = Size(width, bookHeight)
        )
    }
}
