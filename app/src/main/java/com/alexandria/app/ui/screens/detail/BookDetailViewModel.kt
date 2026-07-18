package com.alexandria.app.ui.screens.detail

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

data class DetailUiState(
    val book: Book? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _uiState.value = DetailUiState(
                    book = book,
                    isLoading = false
                )
            }
        }
    }

    fun updateStatus(newStatus: ReadingStatus) {
        viewModelScope.launch {
            _uiState.value.book?.let { book ->
                val updatedBook = book.copy(
                    status = newStatus,
                    dateFinished = if (newStatus == ReadingStatus.FINISHED) {
                        System.currentTimeMillis()
                    } else {
                        book.dateFinished
                    }
                )
                repository.updateBook(updatedBook)
            }
        }
    }

    fun updateRating(rating: Float) {
        viewModelScope.launch {
            _uiState.value.book?.let { book ->
                repository.updateBook(book.copy(rating = rating))
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            _uiState.value.book?.let { book ->
                repository.deleteBook(book)
            }
        }
    }
}
