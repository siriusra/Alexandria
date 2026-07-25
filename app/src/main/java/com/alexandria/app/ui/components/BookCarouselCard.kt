package com.alexandria.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus

@Composable
fun CarouselCard(
    book: Book,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "carousel")

    val readingScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "readingScale"
    )
    val finishedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "finishedAlpha"
    )
    val pendingPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pendingPulse"
    )

    val statusColor = when (book.status) {
        ReadingStatus.READING -> Color(0xFF4CAF50)
        ReadingStatus.FINISHED -> Color(0xFF2196F3)
        ReadingStatus.PENDING -> Color(0xFFFF9800)
    }

    Card(
        modifier = modifier
            .width(340.dp)
            .shadow(
                if (book.status == ReadingStatus.READING) 8.dp + 12.dp * readingScale else 16.dp,
                RoundedCornerShape(28.dp),
                ambientColor = if (book.status == ReadingStatus.READING) statusColor.copy(alpha = readingScale * 0.3f) else Color.Black.copy(alpha = 0.15f),
                spotColor = if (book.status == ReadingStatus.READING) statusColor.copy(alpha = readingScale * 0.4f) else Color.Black.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(
                            when (book.status) {
                                ReadingStatus.READING -> 14.dp * readingScale
                                ReadingStatus.FINISHED -> 14.dp
                                ReadingStatus.PENDING -> 14.dp
                            }
                        )
                        .scale(
                            when (book.status) {
                                ReadingStatus.READING -> readingScale
                                ReadingStatus.FINISHED -> 1f
                                ReadingStatus.PENDING -> 1f + (pendingPulse - 0.5f) * 0.3f
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            when (book.status) {
                                ReadingStatus.READING -> statusColor
                                ReadingStatus.FINISHED -> statusColor.copy(alpha = finishedAlpha)
                                ReadingStatus.PENDING -> statusColor.copy(alpha = pendingPulse)
                            }
                        )
                )
                if (book.status == ReadingStatus.FINISHED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(20.dp)
                            .scale(finishedAlpha)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = (1f - finishedAlpha) * 0.3f))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
    }
}
