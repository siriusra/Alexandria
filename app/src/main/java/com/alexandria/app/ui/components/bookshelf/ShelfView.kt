package com.alexandria.app.ui.components.bookshelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.ui.theme.GenreColorMapper

private val WoodDark = Color(0xFF5D4037)
private val WoodMid = Color(0xFF6D4C41)
private val WoodLight = Color(0xFF8D6E63)
private val WoodEdge = Color(0xFF4E342E)
private val BookShadow = Color(0x1A000000)

@Composable
fun ShelfView(
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                books.forEach { book ->
                    BookSpine(
                        book = book,
                        onClick = { onBookClick(book.id) }
                    )
                }
            }
        }

        ShelfBottom()
    }
}

@Composable
private fun BookSpine(
    book: Book,
    onClick: () -> Unit
) {
    val baseColor = GenreColorMapper.colorFor(book.genre)
    val isReading = book.status == ReadingStatus.LEYENDO
    val maxHeight = 380.dp
    val minHeight = 180.dp
    val heightRatio = (book.pageCount?.let { (it.coerceIn(50, 800) - 50) / 750f } ?: 0.5f)
    val bookHeight = minHeight + (maxHeight - minHeight) * heightRatio
    val topPadding = if (isReading) 24.dp else 0.dp

    val spineShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 2.dp, bottomEnd = 2.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Card(
            onClick = onClick,
            shape = spineShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = baseColor),
            modifier = Modifier
                .width(68.dp)
                .height(bookHeight)
                .padding(top = topPadding)
                .drawBehind {
                    val shadowAlpha = if (isReading) 0.25f else 0.15f
                    drawRoundRect(
                        color = BookShadow.copy(alpha = shadowAlpha),
                        topLeft = Offset(2.dp.toPx(), 0f),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (book.coverUrl != null || book.coverLocalPath != null) {
                    AsyncImage(
                        model = book.coverUrl ?: book.coverLocalPath,
                        contentDescription = book.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(spineShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    baseColor.copy(alpha = 0.6f),
                                    baseColor.copy(alpha = 0.9f),
                                    baseColor
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp
                    )
                    Text(
                        text = book.author,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        if (isReading) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "• leyendo",
                fontSize = 8.sp,
                color = WoodDark,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShelfBottom() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val shelfTop = 0f
        val shelfHeight = 12.dp.toPx()
        val bevelHeight = 4.dp.toPx()
        val totalHeight = shelfHeight + bevelHeight

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(WoodDark, WoodMid, WoodLight, WoodMid, WoodDark)
            ),
            topLeft = Offset(0f, shelfTop),
            size = Size(size.width, shelfHeight)
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(WoodEdge, WoodDark, WoodMid, WoodDark, WoodEdge)
            ),
            topLeft = Offset(0f, shelfTop + shelfHeight),
            size = Size(size.width, bevelHeight)
        )

        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, shelfTop + 1.dp.toPx()),
            end = Offset(size.width, shelfTop + 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        drawLine(
            color = BookShadow,
            start = Offset(0f, shelfTop),
            end = Offset(size.width, shelfTop),
            strokeWidth = 2.dp.toPx()
        )
    }
}
