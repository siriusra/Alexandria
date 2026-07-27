package com.alexandria.app.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.ui.components.PlaceholderPortada
import com.alexandria.app.ui.components.ReadingStatusBadge
import com.alexandria.app.ui.components.uiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    uiState.book?.let { book ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del libro") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToEdit(book.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    val coverUrl = uiState.coverUrl ?: book.coverUrl ?: book.coverLocalPath
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
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
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReadingStatusBadge(status = book.status)

                        if (book.year != null) {
                            AssistChip(
                                onClick = { },
                                label = { Text(book.year.toString()) }
                            )
                        }

                        if (book.pageCount != null) {
                            AssistChip(
                                onClick = { },
                                label = { Text("${book.pageCount} págs") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Género",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = book.genre,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        tonalElevation = 2.dp,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            when {
                                uiState.isDescriptionLoading -> {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                }
                                else -> {
                                    AnimatedVisibility(
                                        visible = uiState.description != null,
                                        enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f)) +
                                                slideInVertically(
                                                    initialOffsetY = { it / 4 },
                                                    animationSpec = spring(dampingRatio = 0.8f)
                                                )
                                    ) {
                                        Text(
                                            text = uiState.description ?: "No hay sinopsis disponible para este libro",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (uiState.description == null)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.externalRating != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val animRating = animateFloatAsState(
                            targetValue = uiState.externalRating!!.toFloat(),
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
                            label = "rating"
                        )
                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (pressed) 0.97f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f),
                            label = "scale"
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            tonalElevation = 1.dp,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                pressed = true
                                                tryAwaitRelease()
                                                pressed = false
                                            }
                                        )
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "%.1f".format(animRating.value),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val fullStars = uiState.externalRating!!.toInt()
                                        repeat(5) { i ->
                                            Icon(
                                                imageVector = if (i < fullStars) {
                                                    Icons.Default.Star
                                                } else {
                                                    Icons.Default.StarBorder
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (uiState.externalRatingsCount != null) {
                                        Text(
                                            text = "${uiState.externalRatingsCount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "valoraciones",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (uiState.ratingSource != null) {
                                        Text(
                                            text = uiState.ratingSource!!,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (book.seriesName != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Saga",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = buildString {
                                append(book.seriesName)
                                if (book.seriesOrder != null) {
                                    append(" #${book.seriesOrder}")
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Estado de lectura",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val currentConfig = book.status.uiConfig()
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = book.status.displayName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = currentConfig.icon,
                                    contentDescription = null,
                                    tint = currentConfig.color
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ReadingStatus.entries.forEach { status ->
                                val cfg = status.uiConfig()
                                DropdownMenuItem(
                                    onClick = {
                                        viewModel.updateStatus(status)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = cfg.icon,
                                            contentDescription = null,
                                            tint = cfg.color
                                        )
                                    },
                                    text = { Text(status.displayName) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Progreso de lectura",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val pageCount = book.pageCount
                    val progress = if (pageCount != null && pageCount > 0) {
                        (book.currentPage.toFloat() / pageCount).coerceIn(0f, 1f)
                    } else null

                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var pageText by remember(book.currentPage) {
                            mutableStateOf(book.currentPage.toString())
                        }

                        OutlinedButton(
                            onClick = {
                                val newPage = (pageText.toIntOrNull() ?: 0).coerceAtLeast(1) - 1
                                pageText = newPage.toString()
                                viewModel.updateCurrentPage(newPage)
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar página")
                        }

                        OutlinedTextField(
                            value = pageText,
                            onValueChange = { value ->
                                val filtered = value.filter { it.isDigit() }
                                pageText = filtered
                                val page = filtered.toIntOrNull()
                                if (page != null) {
                                    val maxPage = pageCount ?: Int.MAX_VALUE
                                    val clamped = page.coerceAtMost(maxPage)
                                    viewModel.updateCurrentPage(clamped)
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        )

                        OutlinedButton(
                            onClick = {
                                val maxPage = pageCount ?: Int.MAX_VALUE
                                val newPage = ((pageText.toIntOrNull() ?: 0) + 1).coerceAtMost(maxPage)
                                pageText = newPage.toString()
                                viewModel.updateCurrentPage(newPage)
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir página")
                        }

                        if (pageCount != null) {
                            Text(
                                text = "/ $pageCount",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (progress != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% completado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (book.rating != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Valoración",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { index ->
                                IconButton(
                                    onClick = {
                                        viewModel.updateRating(
                                            if (book.rating == (index + 1).toFloat()) {
                                                0f
                                            } else {
                                                (index + 1).toFloat()
                                            }
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (index < (book.rating ?: 0f)) {
                                            Icons.Default.Star
                                        } else {
                                            Icons.Default.StarBorder
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (book.notes != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Notas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = book.notes,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar libro") },
                text = { Text("¿Estás seguro de que quieres eliminar \"${book.title}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBook()
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
