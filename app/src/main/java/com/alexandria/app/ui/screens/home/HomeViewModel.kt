package com.alexandria.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val currentlyReading: List<Book> = emptyList(),
    val recentlyAdded: List<Book> = emptyList(),
    val totalBooks: Int = 0,
    val readingCount: Int = 0,
    val finishedCount: Int = 0,
    val pendingCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllBooks().collect { allBooks ->
                _uiState.value = HomeUiState(
                    currentlyReading = allBooks.filter { it.status == ReadingStatus.READING },
                    recentlyAdded = allBooks.sortedByDescending { it.dateAdded }.take(10),
                    totalBooks = allBooks.size,
                    readingCount = allBooks.count { it.status == ReadingStatus.READING },
                    finishedCount = allBooks.count { it.status == ReadingStatus.FINISHED },
                    pendingCount = allBooks.count { it.status == ReadingStatus.PENDING }
                )
            }
        }
    }
}
