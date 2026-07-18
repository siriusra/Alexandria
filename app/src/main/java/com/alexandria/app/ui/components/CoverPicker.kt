package com.alexandria.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alexandria.app.data.remote.GoogleBookItem

@Composable
fun CoverPicker(
    currentCoverUrl: String?,
    onCoverSelected: (String) -> Unit,
    onLocalImageSelected: (Uri) -> Unit,
    searchResults: List<GoogleBookItem>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onLocalImageSelected(it) }
    }

    Column(modifier = modifier) {
        Text(
            text = "Portada del libro",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { showSearchDialog = true }
        ) {
            if (currentCoverUrl != null) {
                AsyncImage(
                    model = currentCoverUrl,
                    contentDescription = "Portada actual",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca para añadir portada",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { showSearchDialog = true }
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buscar online")
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Galería")
            }
        }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Buscar portada") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Título o autor...") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onSearch(searchQuery) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = searchQuery.isNotBlank() && !isSearching
                    ) {
                        Text("Buscar")
                    }

                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (searchResults.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(searchResults) { item ->
                                Card(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clickable {
                                            item.volumeInfo.imageLinks?.thumbnail?.let { url ->
                                                val cleanUrl = url
                                                    .replace("http://", "https://")
                                                    .replace("edge=curl", "edge=curl&fife=w400-h600")
                                                onCoverSelected(cleanUrl)
                                            }
                                            showSearchDialog = false
                                        }
                                ) {
                                    AsyncImage(
                                        model = item.volumeInfo.imageLinks?.thumbnail?.replace(
                                            "http://", "https://"
                                        ),
                                        contentDescription = item.volumeInfo.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    } else if (!isSearching && searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron portadas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
