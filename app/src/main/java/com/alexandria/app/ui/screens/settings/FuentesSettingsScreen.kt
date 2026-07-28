package com.alexandria.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandria.app.data.model.CoverSource
import com.alexandria.app.data.model.CoverSourceConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuentesSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuentes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Configura las fuentes para sinopsis y portadas. Arrastra para cambiar prioridad (arriba = mayor prioridad).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item { SectionHeader(title = "Portadas") }

            val enabledSources = uiState.coverSourcesConfig.enabledSources
            items(enabledSources.size) { index ->
                val source = enabledSources[index]
                CoverSourceRow(
                    source = source,
                    position = index,
                    totalCount = enabledSources.size,
                    isEnabled = true,
                    onToggle = { viewModel.toggleCoverSource(source) },
                    onMoveUp = { if (index > 0) viewModel.moveCoverSource(index, index - 1) },
                    onMoveDown = { if (index < enabledSources.lastIndex) viewModel.moveCoverSource(index, index + 1) }
                )
                if (index < enabledSources.lastIndex) {
                    DividerItem()
                }
            }

            val disabledSources = CoverSource.values().filterNot { it in uiState.coverSourcesConfig.enabledSources }
            if (disabledSources.isNotEmpty()) {
                item {
                    if (enabledSources.isNotEmpty()) {
                        DividerWithLabel()
                    }
                }
                items(disabledSources.size) { index ->
                    val source = disabledSources[index]
                    CoverSourceRow(
                        source = source,
                        position = 0,
                        totalCount = 1,
                        isEnabled = false,
                        onToggle = { viewModel.enableCoverSource(source) },
                        onMoveUp = null,
                        onMoveDown = null
                    )
                    if (index < disabledSources.lastIndex) {
                        DividerItem()
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Caché local de portadas (30 días)", style = MaterialTheme.typography.bodyLarge)
                        Text("Evita descargas repetidas y acelera la carga", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.coverSourcesConfig.cacheEnabled,
                        onCheckedChange = { viewModel.setCoverCacheEnabled(it) }
                    )
                }
            }

            item { SectionHeader(title = "Sinopsis") }

            item {
                SynopsisSourcesList(
                    synopsisSources = uiState.synopsisSources,
                    onToggle = { key -> viewModel.toggleSynopsisSource(key) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Sinopsis: el orden determina qué fuente se consulta primero. Si una falla, se prueba la siguiente.\n" +
                    "Portadas: prioridad de arriba a abajo. Caché evita descargas repetidas (30 días).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DividerItem() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun DividerWithLabel() {
    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            "Inactivas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    HorizontalDivider()
}

@Composable
fun SynopsisSourcesList(
    synopsisSources: com.alexandria.app.data.local.SynopsisSourceConfig,
    onToggle: (String) -> Unit
) {
    val sources = listOf(
        SourceOption("isbn", "ISBN (OpenLibrary)", "Búsqueda directa por ISBN"),
        SourceOption("todostuslibros", "TodosTusLibros", "Sinopsis de todostuslibros.com"),
        SourceOption("casa_del_libro", "Casa del Libro", "Sinopsis de casadellibro.com"),
        SourceOption("openlibrary", "OpenLibrary (español)", "OpenLibrary filtrado por idioma"),
        SourceOption("wikipedia", "Wikipedia", "Wikipedia en español"),
        SourceOption("google_books", "Google Books", "Google Books con restricción de idioma")
    )

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        sources.forEachIndexed { index, source ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val isChecked = when (source.key) {
                "isbn" -> synopsisSources.isbn
                "todostuslibros" -> synopsisSources.todostuslibros
                "casa_del_libro" -> synopsisSources.casaDelLibro
                "openlibrary" -> synopsisSources.openLibrary
                "wikipedia" -> synopsisSources.wikipedia
                "google_books" -> synopsisSources.googleBooks
                else -> false
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(source.key) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(source.label, style = MaterialTheme.typography.bodyLarge)
                    Text(source.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Checkbox(checked = isChecked, onCheckedChange = { onToggle(source.key) })
            }
        }
    }
}

@Composable
fun CoverSourceRow(
    source: CoverSource,
    position: Int,
    totalCount: Int,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnabled) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Arrastrar para reordenar",
                modifier = Modifier.size(24.dp).padding(end = 12.dp).fillMaxHeight()
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp).padding(end = 12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(source.label, style = MaterialTheme.typography.bodyLarge)
                if (!isEnabled) {
                    Text(" (inactiva)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            Text(source.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isEnabled) {
                // Move buttons would go here
            }
            Checkbox(checked = true, onCheckedChange = { })
        }
    }
}
