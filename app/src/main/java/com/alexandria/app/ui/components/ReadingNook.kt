package com.alexandria.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alexandria.app.domain.model.Book
import com.alexandria.app.ui.theme.LocalAlexandriaShapes

@Composable
fun ReadingNook(
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (books.isEmpty()) return

    val shapes = LocalAlexandriaShapes.current
    val book = books.first()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.4f)
            drawRect(
                brush = ShaderBrush(
                    RadialGradientShader(
                        center = center,
                        radius = size.height * 0.6f,
                        colors = listOf(
                            Color(0xFFFFF3E0).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                ),
                size = size
            )
        }

        Surface(
            onClick = { onBookClick(book.id) },
            shape = shapes.cardShape,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(shapes.cardShapeSmall)
                ) {
                    if (book.coverUrl != null || book.coverLocalPath != null) {
                        AsyncImage(
                            model = book.coverUrl ?: book.coverLocalPath,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PlaceholderPortada(
                            titulo = book.title,
                            autor = book.author,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReadingStatusBadge(status = book.status)
                }
            }
        }

        if (books.size > 1) {
            Text(
                text = "+${books.size - 1} más",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 8.dp)
            )
        }
    }
}
