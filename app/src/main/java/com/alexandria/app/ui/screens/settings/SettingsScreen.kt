package com.alexandria.app.ui.screens.settings

import android.content.Intent
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandria.app.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportMethodPicker by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromJson(it, context) }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromCsv(it, context) }
    }

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportJsonViaSAF(it, context) }
    }

    val safCsvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsvViaSAF(it, context) }
    }

    val clipboardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustes") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Modo oscuro")
                        Text(
                            text = if (uiState.isDarkTheme) "Activado" else "Desactivado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Datos", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar biblioteca")
                    }

                    HorizontalDivider()

                    TextButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar biblioteca")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Versión actual")
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { viewModel.checkForUpdate() },
                            enabled = !uiState.isCheckingUpdate && !uiState.isDownloading
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Buscar actualizaciones")
                        }
                    }

                    if (uiState.updateInfo != null) {
                        HorizontalDivider()
                        Column {
                            Text(
                                text = "Nueva versión: v${uiState.updateInfo!!.versionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (uiState.updateInfo!!.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.updateInfo!!.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.isDownloading) {
                                Column {
                                    LinearProgressIndicator(progress = { uiState.downloadProgress }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Descargando... ${(uiState.downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { viewModel.dismissUpdate() }) { Text("Ignorar") }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { viewModel.downloadAndInstall(context) }) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Actualizar")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.updateError != null && uiState.updateInfo == null) {
                        Text(uiState.updateError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Acerca de", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alexandria")
                    Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tu biblioteca virtual personal", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false; },
            title = { Text("Exportar biblioteca") },
            text = {
                Column {
                    Text("Selecciona el destino:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Guardar en una ubicación:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; safExportLauncher.launch("alexandria_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar como JSON...") }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; safCsvExportLauncher.launch("alexandria_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar como CSV...") }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Compartir:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; viewModel.shareJson(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir JSON...") }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; viewModel.shareCsv(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir CSV...") }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Exportación rápida:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; viewModel.exportJsonToDownloads() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("JSON a Descargas") }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showExportDialog = false; viewModel.exportCsvToDownloads() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CSV a Descargas") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false; }) { Text("Cancelar") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar biblioteca") },
            text = {
                Column {
                    Text("Selecciona el origen:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Desde archivo:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain")); showImportDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar JSON (desde Alexandria)") }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")); showImportDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar CSV (desde Excel)") }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Desde portapapeles:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showImportDialog = false; viewModel.importFromClipboard(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pegar JSON del portapapeles") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
