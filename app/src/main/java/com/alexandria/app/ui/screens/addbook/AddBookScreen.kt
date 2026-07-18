package com.alexandria.app.ui.screens.addbook

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.ui.components.CoverPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onCoverUrlChange(it.toString())
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir libro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveBook() },
                        enabled = uiState.title.isNotBlank() && uiState.author.isNotBlank() && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverPicker(
                currentCoverUrl = uiState.coverUrl,
                onCoverSelected = { viewModel.onCoverUrlChange(it) },
                onLocalImageSelected = { viewModel.onCoverUrlChange(it.toString()) },
                searchResults = uiState.coverSearchResults,
                isSearching = uiState.isSearchingCover,
                onSearch = { viewModel.searchCovers(it) },
                coverProvider = uiState.coverProvider,
                onProviderChange = { viewModel.onCoverProviderChange(it) },
                errorMessage = uiState.coverSearchError
            )

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título *") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.author,
                onValueChange = { viewModel.onAuthorChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Autor *") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.genre,
                onValueChange = { viewModel.onGenreChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Género") },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.seriesName,
                    onValueChange = { viewModel.onSeriesNameChange(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Saga") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.seriesOrder,
                    onValueChange = { viewModel.onSeriesOrderChange(it) },
                    modifier = Modifier.weight(0.5f),
                    label = { Text("Nº") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.year,
                    onValueChange = { viewModel.onYearChange(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Año") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.pageCount,
                    onValueChange = { viewModel.onPageCountChange(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Páginas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Text(
                text = "Estado de lectura",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.status == status,
                        onClick = { viewModel.onStatusChange(status) },
                        label = { Text(status.displayName) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.isbn,
                onValueChange = { viewModel.onIsbnChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ISBN") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notas personales") },
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
