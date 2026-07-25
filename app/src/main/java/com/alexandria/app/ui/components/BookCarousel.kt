package com.alexandria.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.alexandria.app.domain.model.Book
import kotlin.math.absoluteValue

sealed class CarouselItem {
    data class Single(val book: Book) : CarouselItem()
    data class Series(val seriesName: String, val books: List<Book>) : CarouselItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCarousel(
    items: List<CarouselItem>,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        pageCount = { items.size },
        initialPage = 0
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1)
                ),
                beyondBoundsPageCount = 1
            ) { page ->
                val pageOffset = pagerState.calculateCurrentOffsetForPage(page)
                val scaleFactor = 1f - (pageOffset.absoluteValue * 0.15f).coerceIn(0f, 0.15f)
                val alphaFactor = 1f - (pageOffset.absoluteValue * 0.4f).coerceIn(0f, 0.4f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                            alpha = alphaFactor
                            translationX = pageOffset * 60f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (val item = items[page]) {
                        is CarouselItem.Single -> {
                            CarouselCard(
                                book = item.book,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            )
                        }
                        is CarouselItem.Series -> {
                            SagaCarouselCard(
                                seriesName = item.seriesName,
                                books = item.books,
                                onBookClick = onBookClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pageCount = items.size
            val primaryColor = MaterialTheme.colorScheme.primary
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            for (i in 0 until pageCount.coerceAtMost(10)) {
                val isSelected = pagerState.currentPage == i
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 6.dp,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                    label = "dotSize"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) primaryColor else onSurfaceColor.copy(alpha = 0.3f),
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .drawBehind {
                            drawRoundRect(
                                color = dotColor,
                                cornerRadius = CornerRadius(size.width / 2f, size.height / 2f)
                            )
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.pager.PagerState.calculateCurrentOffsetForPage(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}
