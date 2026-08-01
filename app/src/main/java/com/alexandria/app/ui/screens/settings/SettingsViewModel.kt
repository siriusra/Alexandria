package com.alexandria.app.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.BuildConfig
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.model.CoverSource
import com.alexandria.app.data.model.CoverSourceConfig
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.data.repository.BookRepository
import com.alexandria.app.update.UpdateChecker
import com.alexandria.app.update.UpdateInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val accentColorIndex: Int = 0,
    val visualMode: VisualMode = VisualMode.CLASSIC,
    val synopsisSources: com.alexandria.app.data.local.SynopsisSourceConfig = com.alexandria.app.data.local.SynopsisSourceConfig(),
    val coverSourcesConfig: CoverSourceConfig = CoverSourceConfig(),
    val pushNotificationsEnabled: Boolean = true,
    val exportMessage: String? = null,
    val updateInfo: UpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BookRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.isDarkTheme.collect { isDark ->
                _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
            }
        }
        viewModelScope.launch {
            preferencesManager.accentColorIndex.collect { index ->
                _uiState.value = _uiState.value.copy(accentColorIndex = index)
            }
        }
        viewModelScope.launch {
            preferencesManager.synopsisSources.collect { sources ->
                _uiState.value = _uiState.value.copy(synopsisSources = sources)
            }
        }
        viewModelScope.launch {
            preferencesManager.coverSourcesConfig.collect { config ->
                _uiState.value = _uiState.value.copy(coverSourcesConfig = config)
            }
        }
        viewModelScope.launch {
            preferencesManager.coverCacheEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(coverSourcesConfig = _uiState.value.coverSourcesConfig.copy(cacheEnabled = enabled))
            }
        }
        viewModelScope.launch {
            preferencesManager.visualMode.collect { mode ->
                _uiState.value = _uiState.value.copy(visualMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesManager.pushNotificationsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(pushNotificationsEnabled = enabled)
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            preferencesManager.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    fun setAccentColorIndex(index: Int) {
        viewModelScope.launch {
            preferencesManager.setAccentColorIndex(index)
        }
    }

    fun setVisualMode(mode: VisualMode) {
        viewModelScope.launch {
            preferencesManager.setVisualMode(mode)
        }
    }

    fun toggleSynopsisSource(source: String) {
        viewModelScope.launch {
            val current = _uiState.value.synopsisSources
            val newConfig = current.toggleSource(source)
            if (newConfig.enabledSources.isNotEmpty()) {
                preferencesManager.setSynopsisSourcesConfig(newConfig)
            }
        }
    }

    fun moveSynopsisSource(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = _uiState.value.synopsisSources
            if (fromIndex in current.enabledSources.indices && toIndex in current.enabledSources.indices) {
                val newConfig = current.moveSource(fromIndex, toIndex)
                preferencesManager.setSynopsisSourcesConfig(newConfig)
            }
        }
    }

    fun toggleCoverSource(source: CoverSource) {
        viewModelScope.launch {
            val current = _uiState.value.coverSourcesConfig
            val newConfig = current.toggleSource(source)
            if (newConfig.enabledSources.isNotEmpty()) {
                preferencesManager.setCoverSourcesConfig(newConfig)
            }
        }
    }

    fun moveCoverSource(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = _uiState.value.coverSourcesConfig
            if (fromIndex in current.enabledSources.indices && toIndex in current.enabledSources.indices) {
                val newConfig = current.moveSource(fromIndex, toIndex)
                preferencesManager.setCoverSourcesConfig(newConfig)
            }
        }
    }

    fun enableCoverSource(source: CoverSource) {
        viewModelScope.launch {
            val current = _uiState.value.coverSourcesConfig
            val newConfig = current.toggleSource(source)
            preferencesManager.setCoverSourcesConfig(newConfig)
        }
    }

    fun setCoverCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCoverCacheEnabled(enabled)
        }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPushNotificationsEnabled(enabled)
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingUpdate = true,
                updateError = null,
                updateInfo = null
            )

            val updateInfo = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)

            _uiState.value = _uiState.value.copy(
                isCheckingUpdate = false,
                updateInfo = updateInfo,
                updateError = if (updateInfo == null) null else null
            )

            if (updateInfo == null) {
                _uiState.value = _uiState.value.copy(
                    updateError = "No hay actualizaciones disponibles"
                )
            }
        }
    }

    fun downloadAndInstall(context: Context) {
        val updateInfo = _uiState.value.updateInfo ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                updateError = null
            )

            val success = UpdateChecker.downloadAndInstall(
                context = context,
                downloadUrl = updateInfo.downloadUrl
            ) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }

            _uiState.value = _uiState.value.copy(isDownloading = false)

            if (!success) {
                _uiState.value = _uiState.value.copy(
                    updateError = "Error al descargar la actualización"
                )
            }
        }
    }

    fun dismissUpdate() {
        _uiState.value = _uiState.value.copy(
            updateInfo = null,
            updateError = null
        )
    }

    fun exportJsonToDownloads() {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val json = Gson().toJson(books)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(dir, "alexandria_$timestamp.json")
                file.writeText(json)
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Guardado en Descargas/${file.name}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun exportCsvToDownloads() {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val csv = buildCsv(books)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(dir, "alexandria_$timestamp.csv")
                file.writeText(csv)
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Guardado en Descargas/${file.name}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun exportJsonViaSAF(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val json = Gson().toJson(books)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Exportado correctamente"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun exportCsvViaSAF(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val csv = buildCsv(books)
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Exportado correctamente"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun shareJson(context: Context) {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val json = Gson().toJson(books)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()
                val file = File(exportDir, "alexandria_$timestamp.json")
                file.writeText(json)
                shareFile(context, file, "application/json")
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Archivo listo para compartir"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al compartir: ${e.message}"
                )
            }
        }
    }

    fun shareCsv(context: Context) {
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks().first()
                val csv = buildCsv(books)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()
                val file = File(exportDir, "alexandria_$timestamp.csv")
                file.writeText(csv)
                shareFile(context, file, "text/csv")
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Archivo listo para compartir"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al compartir: ${e.message}"
                )
            }
        }
    }

    fun importFromJson(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("No se pudo abrir el archivo")

                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                reader.close()

                val type = object : TypeToken<List<Book>>() {}.type
                val books: List<Book> = Gson().fromJson(json, type)

                var importedCount = 0
                books.forEach { book ->
                    repository.addBook(book.withDefaults().copy(id = 0))
                    importedCount++
                }

                _uiState.value = _uiState.value.copy(
                    exportMessage = "Importados $importedCount libros"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al importar: ${e.message}"
                )
            }
        }
    }

    fun importFromCsv(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("No se pudo abrir el archivo")

                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()
                reader.close()

                if (lines.size < 2) throw Exception("El archivo CSV está vacío")

                var importedCount = 0
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.isNotBlank()) {
                        val fields = parseCsvLine(line)
                        if (fields.size >= 11 && fields[0].isNotBlank() && fields[1].isNotBlank()) {
                            val book = Book(
                                title = fields[0],
                                author = fields[1],
                                genre = fields[2].ifBlank { "Sin género" },
                                seriesName = fields[3].ifBlank { null },
                                seriesOrder = fields[4].toIntOrNull(),
                                year = fields[5].toIntOrNull(),
                                status = ReadingStatus.fromString(fields[6]),
                                rating = fields[7].toFloatOrNull(),
                                pageCount = fields[8].toIntOrNull(),
                                isbn = fields[9].ifBlank { null },
                                dateAdded = fields[10].toLongOrNull() ?: System.currentTimeMillis()
                            )
                            repository.addBook(book)
                            importedCount++
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    exportMessage = "Importados $importedCount libros"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al importar: ${e.message}"
                )
            }
        }
    }

    fun importFromClipboard(context: Context) {
        viewModelScope.launch {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: throw Exception("No se pudo acceder al portapapeles")
                val clip = clipboard.primaryClip ?: throw Exception("El portapapeles está vacío")
                val text = clip.getItemAt(0).text?.toString()
                    ?: throw Exception("No hay texto en el portapapeles")

                val type = object : TypeToken<List<Book>>() {}.type
                val books: List<Book> = Gson().fromJson(text, type)

                var importedCount = 0
                books.forEach { book ->
                    repository.addBook(book.withDefaults().copy(id = 0))
                    importedCount++
                }

                _uiState.value = _uiState.value.copy(
                    exportMessage = "Importados $importedCount libros desde portapapeles"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al importar: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    private fun buildCsv(books: List<Book>): String {
        return buildString {
            appendLine("Título,Autor,Género,Saga,Nº,Año,Estado,Valoración,Páginas,ISBN,Fecha añadido")
            books.forEach { book ->
                appendLine(
                    listOf(
                        book.title.escapeCsv(),
                        book.author.escapeCsv(),
                        book.genre?.escapeCsv() ?: "",
                        book.seriesName?.escapeCsv() ?: "",
                        book.seriesOrder?.toString() ?: "",
                        book.year?.toString() ?: "",
                        book.status.displayName,
                        book.rating?.toString() ?: "",
                        book.pageCount?.toString() ?: "",
                        book.isbn?.escapeCsv() ?: "",
                        book.dateAdded.toString()
                    ).joinToString(",")
                )
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir biblioteca"))
    }

    private fun String.escapeCsv(): String {
        return if (contains(",") || contains("\"") || contains("\n")) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
