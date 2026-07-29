package com.alexandria.app.ui.components.bookshelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.ui.theme.GenreColorMapper
import com.alexandria.app.ui.theme.LocalAlexandriaShapes

@Composable
fun ShelfView(
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = LocalAlexandriaShapes.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                books.forEach { book ->
                    val spineColor = GenreColorMapper.colorFor(book.genre)
                    val spineWidth = 36.dp

                    Surface(
                        onClick = { onBookClick(book.id) },
                        shape = shapes.shelfCardShape,
                        color = spineColor,
                        modifier = Modifier
                            .width(spineWidth)
                            .fillMaxHeight()
                            .then(
                                if (book.status == ReadingStatus.LEYENDO) {
                                    Modifier.padding(top = 8.dp)
                                } else Modifier
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = book.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.rotate(-90f)
                            )
                        }
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            drawShelfLine(size)
        }
    }
}

private fun DrawScope.drawShelfLine(size: Size) {
    val shelfColor = GenreColorMapper.shelfColor
    drawRect(
        color = shelfColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, 4.dp.toPx())
    )
    drawRect(
        color = Color.Black.copy(alpha = 0.06f),
        topLeft = Offset(0f, 4.dp.toPx()),
        size = Size(size.width, 2.dp.toPx())
    )
}
