package com.alexandria.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class SourceOption(
    val key: String,
    val label: String,
    val description: String
)

private val sources = listOf(
    SourceOption("isbn", "ISBN (OpenLibrary)", "Búsqueda directa por ISBN"),
    SourceOption("todostuslibros", "TodosTusLibros", "Sinopsis de todostuslibros.com"),
    SourceOption("casa_del_libro", "Casa del Libro", "Sinopsis de casadellibro.com"),
    SourceOption("openlibrary", "OpenLibrary (español)", "OpenLibrary filtrado por idioma"),
    SourceOption("wikipedia", "Wikipedia", "Wikipedia en español"),
    SourceOption("google_books", "Google Books", "Google Books con restricción de idioma")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynopsisSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuentes de sinopsis") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Selecciona las fuentes para obtener sinopsis:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val checked = uiState.synopsisSources
                    sources.forEachIndexed { index, source ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        val isChecked = when (source.key) {
                            "isbn" -> checked.isbn
                            "todostuslibros" -> checked.todostuslibros
                            "casa_del_libro" -> checked.casaDelLibro
                            "openlibrary" -> checked.openLibrary
                            "wikipedia" -> checked.wikipedia
                            "google_books" -> checked.googleBooks
                            else -> false
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSynopsisSource(source.key) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    source.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    source.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { viewModel.toggleSynopsisSource(source.key) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "El orden de consulta es: ISBN → TodosTusLibros → Casa del Libro → OpenLibrary → Wikipedia → Google Books.\n"
                        + "Las fuentes desactivadas se saltan automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}