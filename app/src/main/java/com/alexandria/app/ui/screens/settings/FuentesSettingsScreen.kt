package com.alexandria.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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

            val synopsisMeta = mapOf(
                "isbn" to SourceOption("isbn", "ISBN (OpenLibrary)", "Búsqueda directa por ISBN"),
                "bne" to SourceOption("bne", "BNE (España)", "Catálogo oficial de la Biblioteca Nacional de España (SPARQL)"),
                "openlibrary" to SourceOption("openlibrary", "OpenLibrary (español)", "OpenLibrary filtrado por idioma"),
                "wikipedia" to SourceOption("wikipedia", "Wikipedia", "Wikipedia en español"),
                "google_books" to SourceOption("google_books", "Google Books", "Google Books con restricción de idioma")
            )
            val synopsisEnabled = uiState.synopsisSources.enabledSources
            items(synopsisEnabled.size) { index ->
                val key = synopsisEnabled[index]
                val meta = synopsisMeta[key] ?: return@items
                SynopsisSourceRow(
                    label = meta.label,
                    description = meta.description,
                    position = index,
                    totalCount = synopsisEnabled.size,
                    onToggle = { viewModel.toggleSynopsisSource(key) },
                    onMoveUp = { if (index > 0) viewModel.moveSynopsisSource(index, index - 1) },
                    onMoveDown = { if (index < synopsisEnabled.lastIndex) viewModel.moveSynopsisSource(index, index + 1) }
                )
                if (index < synopsisEnabled.lastIndex) {
                    DividerItem()
                }
            }

            val allSynopsisKeys = listOf("isbn", "bne", "openlibrary", "wikipedia", "google_books")
            val synopsisDisabled = allSynopsisKeys.filterNot { it in uiState.synopsisSources.enabledSources }
            if (synopsisDisabled.isNotEmpty()) {
                item {
                    if (synopsisEnabled.isNotEmpty()) {
                        DividerWithLabel()
                    }
                }
                items(synopsisDisabled.size) { index ->
                    val key = synopsisDisabled[index]
                    val meta = synopsisMeta[key] ?: return@items
                    SynopsisSourceRow(
                        label = meta.label,
                        description = meta.description,
                        position = 0,
                        totalCount = 1,
                        isEnabled = false,
                        onToggle = { viewModel.toggleSynopsisSource(key) },
                        onMoveUp = null,
                        onMoveDown = null
                    )
                    if (index < synopsisDisabled.lastIndex) {
                        DividerItem()
                    }
                }
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
fun SynopsisSourceRow(
    label: String,
    description: String,
    position: Int,
    totalCount: Int,
    isEnabled: Boolean = true,
    onToggle: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isEnabled) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Checkbox(checked = isEnabled, onCheckedChange = { onToggle() })
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
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(20.dp))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Checkbox(checked = isEnabled, onCheckedChange = { onToggle() })
        }
    }
}
