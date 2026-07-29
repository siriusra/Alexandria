package com.alexandria.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexandria.app.domain.model.Book
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReadingTimeline(
    finishedBooks: List<Book>,
    modifier: Modifier = Modifier
) {
    if (finishedBooks.isEmpty()) return

    val sortedBooks = finishedBooks
        .filter { it.dateFinished != null }
        .sortedByDescending { it.dateFinished }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Línea de lectura",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        sortedBooks.forEachIndexed { index, book ->
            TimelineItem(
                book = book,
                isLast = index == sortedBooks.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineItem(
    book: Book,
    isLast: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 5.dp.toPx()
                drawCircle(
                    color = primaryColor,
                    radius = dotRadius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )

                if (!isLast) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(size.width / 2f, size.height / 2f + dotRadius + 2.dp.toPx()),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(book.author)
                    if (book.dateFinished != null) {
                        append(" · ")
                        append(dateFormat.format(Date(book.dateFinished)))
                    }
                    if (book.rating != null) {
                        append(" · ★${"%.1f".format(book.rating)}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
