package com.alexandria.app.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.ui.components.BookCarousel
import com.alexandria.app.ui.components.BookGrid
import com.alexandria.app.ui.components.BookList
import com.alexandria.app.ui.components.ViewMode
import com.alexandria.app.ui.components.ViewToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToBookDetail: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                actions = {
                    ViewToggle(
                        viewMode = uiState.viewMode,
                        onModeSelected = { viewModel.setViewMode(it) }
                    )

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filtros"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChips(
                selectedStatus = uiState.selectedStatus,
                onStatusSelected = { viewModel.setStatusFilter(it) }
            )

            if (uiState.books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No hay libros con estos filtros",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                when (uiState.viewMode) {
                    ViewMode.GRID -> {
                        BookGrid(
                            books = uiState.books,
                            onBookClick = onNavigateToBookDetail
                        )
                    }
                    ViewMode.LIST -> {
                        BookList(
                            books = uiState.books,
                            onBookClick = onNavigateToBookDetail
                        )
                    }
                    ViewMode.CAROUSEL -> {
                        BookCarousel(
                            items = uiState.carouselItems,
                            onBookClick = onNavigateToBookDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedStatus: ReadingStatus?,
    onStatusSelected: (ReadingStatus?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("Todos") }
        )

        ReadingStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(status.displayName) }
            )
        }
    }
}
