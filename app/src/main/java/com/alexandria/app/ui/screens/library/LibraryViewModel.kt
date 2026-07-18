package com.alexandria.app.ui.screens.library

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

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllBooks(),
                _isGridView,
                _selectedStatus,
                _selectedGenre,
                _sortBy
            ) { books, isGrid, status, genre, sort ->
                var filtered = books

                status?.let { s ->
                    filtered = filtered.filter { it.status == s }
                }

                genre?.let { g ->
                    filtered = filtered.filter { it.genre == g }
                }

                filtered = when (sort) {
                    SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
                    SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
                    SortOption.AUTHOR -> filtered.sortedBy { it.author.lowercase() }
                    SortOption.RATING -> filtered.sortedByDescending { it.rating ?: 0f }
                }

                LibraryUiState(
                    books = filtered,
                    isGridView = isGrid,
                    selectedStatus = status,
                    selectedGenre = genre,
                    sortBy = sort
                )
            }.collect { state ->
                _uiState.value = state
            }
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
