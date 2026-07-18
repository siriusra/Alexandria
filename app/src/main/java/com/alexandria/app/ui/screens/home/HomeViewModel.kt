package com.alexandria.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
            combine(
                repository.getBooksByStatus(ReadingStatus.READING),
                repository.getAllBooks(),
                repository.getBookCountByStatus(ReadingStatus.READING),
                repository.getBookCountByStatus(ReadingStatus.FINISHED),
                repository.getBookCountByStatus(ReadingStatus.PENDING)
            ) { reading, all, readingCount, finishedCount, pendingCount ->
                HomeUiState(
                    currentlyReading = reading,
                    recentlyAdded = all.take(10),
                    totalBooks = all.size,
                    readingCount = readingCount,
                    finishedCount = finishedCount,
                    pendingCount = pendingCount
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
