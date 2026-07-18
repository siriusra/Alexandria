package com.alexandria.app.ui.screens.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isGridView: Boolean = true,
    val selectedStatus: ReadingStatus? = null,
    val selectedGenre: String? = null,
    val sortBy: SortOption = SortOption.DATE_ADDED
)

enum class SortOption(val displayName: String) {
    DATE_ADDED("Más recientes"),
    TITLE("Título"),
    AUTHOR("Autor"),
    RATING("Valoración")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    private val _selectedStatus = MutableStateFlow<ReadingStatus?>(null)
    private val _selectedGenre = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow(SortOption.DATE_ADDED)

    private var cachedBooks: List<Book> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllBooks().collect { books ->
                cachedBooks = books
                rebuildUiState()
            }
        }

        viewModelScope.launch {
            _sortBy.collect { rebuildUiState() }
        }

        viewModelScope.launch {
            _selectedStatus.collect { rebuildUiState() }
        }

        viewModelScope.launch {
            _selectedGenre.collect { rebuildUiState() }
        }

        viewModelScope.launch {
            _isGridView.collect { rebuildUiState() }
        }
    }

    private fun rebuildUiState() {
        try {
            val books = cachedBooks
            val status = _selectedStatus.value
            val genre = _selectedGenre.value
            val sort = _sortBy.value
            val isGrid = _isGridView.value

            var result: List<Book> = ArrayList(books)

            if (status != null) {
                result = result.filter { it.status == status }
            }

            if (genre != null) {
                result = result.filter { it.genre == genre }
            }

            result = when (sort) {
                SortOption.DATE_ADDED -> result.sortedByDescending { it.dateAdded }
                SortOption.TITLE -> result.sortedBy { it.title.lowercase() }
                SortOption.AUTHOR -> result.sortedBy { it.author.lowercase() }
                SortOption.RATING -> result.sortedByDescending { it.rating ?: 0f }
            }

            _uiState.value = LibraryUiState(
                books = result,
                isGridView = isGrid,
                selectedStatus = status,
                selectedGenre = genre,
                sortBy = sort
            )
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error rebuilding UI state", e)
            _uiState.value = LibraryUiState()
        }
    }

    fun toggleView() {
        _isGridView.value = !_isGridView.value
    }

    fun setStatusFilter(status: ReadingStatus?) {
        _selectedStatus.value = status
    }

    fun setGenreFilter(genre: String?) {
        _selectedGenre.value = genre
    }

    fun setSortOption(option: SortOption) {
        _sortBy.value = option
    }
}
