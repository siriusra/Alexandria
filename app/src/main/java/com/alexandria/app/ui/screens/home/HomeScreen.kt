package com.alexandria.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.components.BookCard
import com.alexandria.app.ui.components.ReadingInsightsCard
import com.alexandria.app.ui.components.ReadingNook
import com.alexandria.app.ui.components.ReadingTimeline
import com.alexandria.app.ui.components.bookshelf.BookshelfEmptyState
import com.alexandria.app.ui.theme.LocalVisualMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddBook: () -> Unit,
    onNavigateToBookDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visualMode = LocalVisualMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alexandria",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBook
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir libro")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (visualMode == VisualMode.IMMERSIVE && uiState.insights.totalBooks > 0) {
                item(key = "insights") {
                    StaggeredSection(0) {
                        ReadingInsightsCard(insights = uiState.insights)
                    }
                }
            } else {
                item(key = "stats") {
                    StaggeredSection(0) {
                        StatsSection(
                            totalBooks = uiState.totalBooks,
                            readingCount = uiState.readingCount,
                            finishedCount = uiState.finishedCount,
                            pendingCount = uiState.pendingCount
                        )
                    }
                }
            }

            if (uiState.currentlyReading.isNotEmpty()) {
                item(key = "reading_header") {
                    StaggeredSection(1) {
                        Text(
                            text = "Leyendo ahora",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item(key = "reading_content") {
                    StaggeredSection(2) {
                        if (visualMode == VisualMode.IMMERSIVE) {
                            ReadingNook(
                                books = uiState.currentlyReading,
                                onBookClick = onNavigateToBookDetail
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.currentlyReading, key = { it.id }) { book ->
                                    BookCard(
                                        book = book,
                                        onClick = { onNavigateToBookDetail(book.id) },
                                        modifier = Modifier.width(180.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (visualMode == VisualMode.IMMERSIVE && uiState.finishedBooks.isNotEmpty()) {
                item(key = "timeline") {
                    StaggeredSection(3) {
                        ReadingTimeline(finishedBooks = uiState.finishedBooks)
                    }
                }
            }

            if (uiState.recentlyAdded.isNotEmpty()) {
                item(key = "recent_header") {
                    StaggeredSection(4) {
                        Text(
                            text = "Añadidos recientemente",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item(key = "recent_content") {
                    StaggeredSection(5) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyAdded, key = { it.id }) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onNavigateToBookDetail(book.id) },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.totalBooks == 0) {
                item(key = "empty") {
                    StaggeredSection(6) {
                        if (visualMode == VisualMode.IMMERSIVE) {
                            BookshelfEmptyState(
                                message = "Tu biblioteca está vacía",
                                actionLabel = "Añadir libro",
                                onAction = onNavigateToAddBook
                            )
                        } else {
                            EmptyState(onNavigateToAddBook)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(
    totalBooks: Int,
    readingCount: Int,
    finishedCount: Int,
    pendingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Total", value = totalBooks)
            StatItem(label = "Leyendo", value = readingCount)
            StatItem(label = "Terminados", value = finishedCount)
            StatItem(label = "Pendientes", value = pendingCount)
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StaggeredSection(
    index: Int,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 100L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 30f
        }
    ) {
        content()
    }
}

@Composable
private fun EmptyState(onNavigateToAddBook: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu biblioteca está vacía",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Añade tu primer libro para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onNavigateToAddBook) {
                Text("Añadir libro")
            }
        }
    }
}
