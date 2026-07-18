package com.alexandria.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.BuildConfig
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
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
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val exportMessage: String? = null,
    val updateInfo: UpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val updateError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleTheme() {
        _uiState.value = _uiState.value.copy(
            isDarkTheme = !_uiState.value.isDarkTheme
        )
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

    fun exportToJson(file: File) {
        viewModelScope.launch {
            try {
                repository.getAllBooks().first().let { books ->
                    val json = Gson().toJson(books)
                    file.writeText(json)
                    _uiState.value = _uiState.value.copy(
                        exportMessage = "Exportado exitosamente a ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun exportToCsv(file: File) {
        viewModelScope.launch {
            try {
                repository.getAllBooks().first().let { books ->
                    val csv = buildString {
                        appendLine("Título,Autor,Género,Saga,Nº,Año,Estado,Valoración,Páginas,ISBN,Fecha añadido")
                        books.forEach { book ->
                            appendLine(
                                listOf(
                                    book.title.escapeCsv(),
                                    book.author.escapeCsv(),
                                    book.genre.escapeCsv(),
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
                    file.writeText(csv)
                    _uiState.value = _uiState.value.copy(
                        exportMessage = "Exportado exitosamente a ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Error al exportar: ${e.message}"
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
                    val bookWithoutId = book.copy(id = 0)
                    repository.addBook(bookWithoutId)
                    importedCount++
                }

                _uiState.value = _uiState.value.copy(
                    exportMessage = "Importados $importedCount libros exitosamente"
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

                if (lines.size < 2) throw Exception("El archivo CSV está vacío o no tiene datos")

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
                    exportMessage = "Importados $importedCount libros exitosamente"
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
