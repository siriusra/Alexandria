package com.alexandria.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SagaCarouselCard(
    seriesName: String,
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleBooks = books.take(4)
    val count = visibleBooks.size
    val isDeck = count > 1

    Card(
        modifier = modifier
            .width(340.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isDeck) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .padding(start = 12.dp, end = 12.dp, top = 16.dp)
                ) {
                    val maxWidth = 316.dp
                    val overlap = when (count) {
                        2 -> 55.dp
                        3 -> 40.dp
                        else -> 25.dp
                    }
                    val cardWidth = ((maxWidth + overlap * (count - 1).toFloat()) / count.toFloat())
                        .coerceIn(90.dp, 150.dp)
                    val step = cardWidth - overlap
                    val startX = (maxWidth - cardWidth - step * (count - 1).toFloat()) / 2f

                    visibleBooks.forEachIndexed { index, book ->
                        val offsetX = startX + step * index.toFloat()
                        val rotation = when (index % 4) {
                            0 -> -1.5f
                            1 -> 2.5f
                            2 -> -2f
                            else -> 1.5f
                        }
                        val yOffset = if (index % 2 == 0) 0.dp else 6.dp

                        DeckBookCover(
                            book = book,
                            index = index,
                            cardWidth = cardWidth,
                            rotation = rotation,
                            yOffset = yOffset,
                            offsetX = offsetX,
                            showSagaBadge = index == 0,
                            onClick = { onBookClick(book.id) }
                        )
                    }

                    if (books.size > 4) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "+${books.size - 4}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.68f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .clickable { onBookClick(visibleBooks.first().id) }
                ) {
                    val coverModel = visibleBooks.first().coverUrl ?: visibleBooks.first().coverLocalPath
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = seriesName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = seriesName.take(1).uppercase(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Saga",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = seriesName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${books.size} libro${if (books.size != 1) "s" else ""} · ${
                        books.count { it.status == ReadingStatus.FINISHED }
                    } leído${if (books.count { it.status == ReadingStatus.FINISHED } != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DeckBookCover(
    book: Book,
    index: Int,
    cardWidth: Dp,
    rotation: Float,
    yOffset: Dp,
    offsetX: Dp,
    showSagaBadge: Boolean,
    onClick: () -> Unit
) {
    val animatedScale = remember { Animatable(0.5f) }
    val animatedAlpha = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        delay(index * 80L)
        launch { animatedScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 250f)) }
        launch { animatedAlpha.animateTo(1f, tween(200)) }
        launch { animatedOffsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
    }

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    val coverModel = book.coverUrl ?: book.coverLocalPath

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = yOffset)
            .width(cardWidth)
            .aspectRatio(0.68f)
            .graphicsLayer {
                scaleX = animatedScale.value * pressScale
                scaleY = animatedScale.value * pressScale
                alpha = animatedAlpha.value
                translationY = animatedOffsetY.value
                rotationZ = rotation * (1f - animatedScale.value)
                shadowElevation = 8f
                shape = RoundedCornerShape(12.dp)
                clip = true
            }
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
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
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )

        Text(
            text = book.title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val statusColor = book.status.uiConfig().color
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )

        if (showSagaBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Saga",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
