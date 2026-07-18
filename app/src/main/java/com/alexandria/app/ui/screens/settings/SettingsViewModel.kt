package com.alexandria.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val exportMessage: String? = null
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
}
