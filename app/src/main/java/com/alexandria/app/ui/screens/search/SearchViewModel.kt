package com.alexandria.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Book> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _query.debounce(300).collectLatest { query ->
                if (query.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        results = emptyList(),
                        isSearching = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSearching = true)
                    repository.searchBooks(query).collect { results ->
                        _uiState.value = _uiState.value.copy(
                            results = results,
                            isSearching = false
                        )
                    }
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
        _uiState.value = _uiState.value.copy(query = query)
    }
}
