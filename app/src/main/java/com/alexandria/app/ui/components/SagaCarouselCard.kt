package com.alexandria.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(if (isExpanded) 420.dp else 340.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clickable { onToggle() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val firstBook = books.firstOrNull()
            val coverModel = firstBook?.coverUrl ?: firstBook?.coverLocalPath

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
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

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clickable { onToggle() },
                    tint = Color.White
                )
            }

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

                if (!isExpanded && books.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        books.take(5).forEach { book ->
                            val coverModel = book.coverUrl ?: book.coverLocalPath
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .aspectRatio(0.68f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shadow(2.dp, RoundedCornerShape(4.dp))
                            ) {
                                if (coverModel != null) {
                                    AsyncImage(
                                        model = coverModel,
                                        contentDescription = null,
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
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy((-20).dp)
                ) {
                    books.forEachIndexed { index, book ->
                        AnimatedDeckCard(
                            book = book,
                            index = index,
                            onClick = { onBookClick(book.id) },
                            modifier = Modifier.zIndex(index.toFloat())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedDeckCard(
    book: Book,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animScale = remember { Animatable(0.3f) }
    val animOffsetY = remember { Animatable(100f) }
    val animAlpha = remember { Animatable(0f) }
    val animRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 70L)
        launch { animScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
        launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
        launch { animAlpha.animateTo(1f, tween(200)) }
        val targetRotation = if (index % 2 == 0) -2f else 2f
        launch { animRotation.animateTo(targetRotation, spring(dampingRatio = 0.7f, stiffness = 200f)) }
    }

    val coverModel = book.coverUrl ?: book.coverLocalPath

    Card(
        modifier = modifier
            .width(130.dp)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
                translationY = animOffsetY.value
                alpha = animAlpha.value
                rotationZ = animRotation.value
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
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
        }
    }
}
