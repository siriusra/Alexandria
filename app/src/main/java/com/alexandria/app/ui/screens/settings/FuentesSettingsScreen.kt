package com.alexandria.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val enabledSources = remember { mutableStateListOf<CoverSource>() }
    val disabledSources = remember { mutableStateListOf<CoverSource>() }
    
    LaunchedEffect(uiState.coverSourcesConfig) {
        enabledSources.clear()
        enabledSources.addAll(uiState.coverSourcesConfig.enabledSources)
        disabledSources.clear()
        disabledSources.addAll(CoverSource.values().filterNot { it in uiState.coverSourcesConfig.enabledSources })
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Configura las fuentes para sinopsis y portadas. Arrastra para cambiar prioridad (arriba = mayor prioridad).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sinopsis section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Sinopsis",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()

                    SynopsisSourcesList(
                        synopsisSources = uiState.synopsisSources,
                        onToggle = { key -> viewModel.toggleSynopsisSource(key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Portadas section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Portadas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()

                    CoverSourcesList(
                        coverConfig = uiState.coverSourcesConfig,
                        onToggle = { source -> viewModel.toggleCoverSource(source) },
                        onMoveUp = { index -> if (index > 0) viewModel.moveCoverSource(index, index - 1) },
                        onMoveDown = { index -> if (index < uiState.coverSourcesConfig.enabledSources.lastIndex) viewModel.moveCoverSource(index, index + 1) },
                        onCacheToggle = { enabled -> viewModel.setCoverCacheEnabled(enabled) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                "Sinopsis: el orden determina qué fuente se consulta primero. Si una falla, se prueba la siguiente.\n" +
                "Portadas: prioridad de arriba a abajo. Caché evita descargas repetidas (30 días).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
                Checkbox(checked = isChecked, onCheckedChange = { _ -> onToggle(source.key) })
            }
        }
    }
}

@Composable
fun CoverSourcesList(
    coverConfig: CoverSourceConfig,
    onToggle: (CoverSource) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCacheToggle: (Boolean) -> Unit
) {
    val enabledSources = remember { mutableStateListOf<CoverSource>() }
    val disabledSources = remember { mutableStateListOf<CoverSource>() }
    
    LaunchedEffect(coverConfig) {
        enabledSources.clear()
        enabledSources.addAll(coverConfig.enabledSources)
        disabledSources.clear()
        disabledSources.addAll(CoverSource.values().filterNot { it in coverConfig.enabledSources })
    }

    Column(modifier = Modifier.padding(4.dp)) {
        // Enabled sources with reorder
        if (enabledSources.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                itemsIndexed(enabledSources) { index, source ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    CoverSourceRow(
                        source = source,
                        position = index,
                        totalCount = enabledSources.size,
                        isEnabled = true,
                        onToggle = { onToggle(source) },
                        onMoveUp = { if (index > 0) onMoveUp(index) },
                        onMoveDown = { if (index < enabledSources.size - 1) onMoveDown(index) }
                    )
                }
            }
        }

        // Disabled sources
        if (disabledSources.isNotEmpty()) {
            if (enabledSources.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    "Inactivas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                itemsIndexed(disabledSources) { index, source ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    CoverSourceRow(
                        source = source,
                        position = index,
                        totalCount = disabledSources.size,
                        isEnabled = false,
                        onToggle = { onToggle(source) },
                        onMoveUp = null,
                        onMoveDown = null
                    )
                }
            }
        }

        // Cache toggle
        Spacer(modifier = Modifier.height(8.dp))
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
                checked = coverConfig.cacheEnabled,
                onCheckedChange = { onCacheToggle(it) }
            )
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
                onMoveUp?.let { up ->
                    IconButton(onClick = up, enabled = position > 0) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir prioridad")
                    }
                }
                onMoveDown?.let { down ->
                    IconButton(onClick = down, enabled = position < totalCount - 1) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar prioridad")
                    }
                }
            }
            Checkbox(checked = isEnabled, onCheckedChange = { _ -> onToggle() })
        }
    }
}