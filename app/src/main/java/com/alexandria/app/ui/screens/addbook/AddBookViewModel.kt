package com.alexandria.app.ui.screens.addbook

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
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
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private val editBookId: Long = savedStateHandle["bookId"] ?: 0L

    init {
        if (editBookId > 0) {
            _uiState.value = _uiState.value.copy(isEditing = true)
            loadBook(editBookId)
        }
    }

    private fun loadBook(bookId: Long) {
        viewModelScope.launch {
            repository.getBookById(bookId).first()?.let { book ->
                _uiState.value = _uiState.value.copy(
                    title = book.title,
                    author = book.author,
                    genre = book.genre,
                    seriesName = book.seriesName ?: "",
                    seriesOrder = book.seriesOrder?.toString() ?: "",
                    year = book.year?.toString() ?: "",
                    status = book.status,
                    coverUrl = book.coverUrl,
                    rating = book.rating,
                    notes = book.notes ?: "",
                    pageCount = book.pageCount?.toString() ?: "",
                    isbn = book.isbn ?: ""
                )
            }
        }
    }

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

    fun saveBook() {
        val state = _uiState.value
        if (state.title.isBlank() || state.author.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val book = Book(
                id = if (state.isEditing) editBookId else 0L,
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

            if (state.isEditing) {
                repository.updateBook(book)
            } else {
                repository.addBook(book)
            }

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedSuccessfully = true
            )
        }
    }
}
