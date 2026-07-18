package com.alexandria.app.ui.screens.addbook

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.CoverProvider
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.remote.GoogleBookItem
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    val genre: String = "",
    val seriesName: String = "",
    val seriesOrder: String = "",
    val year: String = "",
    val status: ReadingStatus = ReadingStatus.PENDING,
    val coverUrl: String? = null,
    val rating: Float? = null,
    val notes: String = "",
    val pageCount: String = "",
    val isbn: String = "",
    val isSaving: Boolean = false,
    val isSearchingCover: Boolean = false,
    val coverSearchResults: List<GoogleBookItem> = emptyList(),
    val coverSearchError: String? = null,
    val coverProvider: CoverProvider = CoverProvider.GOOGLE_BOOKS,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun onAuthorChange(value: String) {
        _uiState.value = _uiState.value.copy(author = value)
    }

    fun onGenreChange(value: String) {
        _uiState.value = _uiState.value.copy(genre = value)
    }

    fun onSeriesNameChange(value: String) {
        _uiState.value = _uiState.value.copy(seriesName = value)
    }

    fun onSeriesOrderChange(value: String) {
        _uiState.value = _uiState.value.copy(seriesOrder = value)
    }

    fun onYearChange(value: String) {
        _uiState.value = _uiState.value.copy(year = value)
    }

    fun onStatusChange(value: ReadingStatus) {
        _uiState.value = _uiState.value.copy(status = value)
    }

    fun onCoverUrlChange(value: String?) {
        _uiState.value = _uiState.value.copy(coverUrl = value)
    }

    fun onRatingChange(value: Float) {
        _uiState.value = _uiState.value.copy(rating = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onPageCountChange(value: String) {
        _uiState.value = _uiState.value.copy(pageCount = value)
    }

    fun onIsbnChange(value: String) {
        _uiState.value = _uiState.value.copy(isbn = value)
    }

    fun onCoverProviderChange(provider: CoverProvider) {
        _uiState.value = _uiState.value.copy(coverProvider = provider)
    }

    fun searchCovers(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearchingCover = true,
                coverSearchError = null
            )
            try {
                val results = repository.searchCovers(query, _uiState.value.coverProvider)
                _uiState.value = _uiState.value.copy(
                    coverSearchResults = results,
                    isSearchingCover = false,
                    coverSearchError = if (results.isEmpty()) "No se encontraron portadas" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    coverSearchResults = emptyList(),
                    isSearchingCover = false,
                    coverSearchError = "Error de búsqueda: ${e.message}"
                )
            }
        }
    }

    fun clearSearchError() {
        _uiState.value = _uiState.value.copy(coverSearchError = null)
    }

    fun saveBook() {
        val state = _uiState.value
        if (state.title.isBlank() || state.author.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val book = Book(
                title = state.title.trim(),
                author = state.author.trim(),
                genre = state.genre.trim().ifBlank { "Sin género" },
                seriesName = state.seriesName.trim().ifBlank { null },
                seriesOrder = state.seriesOrder.toIntOrNull(),
                year = state.year.toIntOrNull(),
                status = state.status,
                coverUrl = state.coverUrl,
                rating = state.rating,
                notes = state.notes.trim().ifBlank { null },
                pageCount = state.pageCount.toIntOrNull(),
                isbn = state.isbn.trim().ifBlank { null }
            )

            repository.addBook(book)

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedSuccessfully = true
            )
        }
    }
}
