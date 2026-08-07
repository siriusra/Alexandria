package com.alexandria.app.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import coil.compose.SubcomposeAsyncImage
import com.alexandria.app.domain.model.BookCharacter
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.components.CharacterAvatar
import com.alexandria.app.ui.components.CharacterEditDialog
import com.alexandria.app.ui.components.CharacterSuggestionsDialog
import com.alexandria.app.ui.components.ICON_TYPE_EMOJI
import com.alexandria.app.ui.components.PlaceholderPortada
import com.alexandria.app.ui.components.ReadingStatusBadge
import com.alexandria.app.ui.components.coverEdgeFade
import com.alexandria.app.ui.components.coverGradientScrim
import com.alexandria.app.ui.components.uiConfig
import com.alexandria.app.ui.theme.LocalVisualMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCharacterAdd by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<BookCharacter?>(null) }

    val currentBook = uiState.book
    if (currentBook != null) {
        val book = currentBook
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
            val scrollState = rememberScrollState()
            val visualMode = LocalVisualMode.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                val coverUrl = uiState.coverUrl ?: book.coverUrl ?: book.coverLocalPath

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (visualMode == VisualMode.IMMERSIVE) {
                                Modifier.heightIn(min = 350.dp)
                            } else {
                                Modifier.height(300.dp)
                            }
                        )
                        .graphicsLayer {
                            translationY = scrollState.value * 0.15f
                        }
                ) {
                    if (coverUrl != null) {
                        SubcomposeAsyncImage(
                            model = coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .coverGradientScrim(book.genre, visualMode),
                            contentScale = ContentScale.Crop,
                            error = {
                                PlaceholderPortada(
                                    titulo = book.title,
                                    autor = book.author,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .coverGradientScrim(book.genre, visualMode)
                                )
                            }
                        )
                    } else {
                        PlaceholderPortada(
                            titulo = book.title,
                            autor = book.author,
                            modifier = Modifier
                                .fillMaxSize()
                                .coverGradientScrim(book.genre, visualMode)
                        )
                    }

                    if (visualMode == VisualMode.IMMERSIVE) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .coverEdgeFade(book.genre, visualMode)
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.titleLarge,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (visualMode != VisualMode.IMMERSIVE) {
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
                    }

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
                        text = book.genre ?: "",
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

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resolveWithAI() },
                                    enabled = !uiState.isCloudResolving
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (uiState.isCloudResolving) "Buscando…" else "Buscar con IA"
                                    )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Personajes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showCharacterAdd = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir")
                        }
                        if (uiState.isCharactersLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            OutlinedButton(onClick = { viewModel.searchCharacters() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buscar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val characters = uiState.characters.sortedWith(
                        compareByDescending<BookCharacter> { it.isFavorite }.thenBy { it.sortOrder }
                    )

                    if (characters.isEmpty()) {
                        Text(
                            text = "Sin personajes todavía. Añade uno a mano o usa «Buscar» para encontrarlos en Wikipedia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        characters.forEach { character ->
                            CharacterRow(
                                character = character,
                                onEdit = { editingCharacter = character },
                                onToggleFavorite = {
                                    viewModel.toggleCharacterFavorite(
                                        character.id,
                                        !character.isFavorite
                                    )
                                },
                                onDelete = { viewModel.deleteCharacter(character.id) }
                            )
                        }
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

        if (showCharacterAdd) {
            CharacterEditDialog(
                title = "Añadir personaje",
                initialName = "",
                initialIconType = ICON_TYPE_EMOJI,
                initialIconKey = "⭐",
                onDismiss = { showCharacterAdd = false },
                onSave = { name, iconType, iconKey ->
                    viewModel.addCharacter(name, iconType, iconKey)
                    showCharacterAdd = false
                }
            )
        }

        editingCharacter?.let { character ->
            CharacterEditDialog(
                title = "Editar personaje",
                initialName = character.name,
                initialIconType = character.iconType,
                initialIconKey = character.iconKey,
                onDismiss = { editingCharacter = null },
                onSave = { name, iconType, iconKey ->
                    viewModel.updateCharacter(character.id, name, iconType, iconKey)
                    editingCharacter = null
                }
            )
        }

        uiState.characterSuggestions?.let { candidates ->
            CharacterSuggestionsDialog(
                candidates = candidates,
                onDismiss = { viewModel.dismissCharacterSuggestions() },
                onAdd = { pairs -> viewModel.addCharacters(pairs) }
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No se encontró el libro",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterRow(
    character: BookCharacter,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterAvatar(
                iconType = character.iconType,
                iconKey = character.iconKey,
                size = 36.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = character.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (character.isFavorite) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (character.isFavorite) {
                        Icons.Default.Star
                    } else {
                        Icons.Default.StarBorder
                    },
                    contentDescription = if (character.isFavorite) {
                        "Quitar de favoritos"
                    } else {
                        "Marcar como favorito"
                    },
                    tint = if (character.isFavorite) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar personaje",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
