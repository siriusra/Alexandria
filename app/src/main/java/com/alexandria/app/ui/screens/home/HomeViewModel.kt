package com.alexandria.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import com.alexandria.app.ui.components.ReadingInsights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class HomeUiState(
    val currentlyReading: List<Book> = emptyList(),
    val recentlyAdded: List<Book> = emptyList(),
    val totalBooks: Int = 0,
    val readingCount: Int = 0,
    val finishedCount: Int = 0,
    val pendingCount: Int = 0,
    val finishedBooks: List<Book> = emptyList(),
    val insights: ReadingInsights = ReadingInsights()
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
                val finished = allBooks.filter { it.status == ReadingStatus.TERMINADO }
                _uiState.value = HomeUiState(
                    currentlyReading = allBooks.filter { it.status == ReadingStatus.LEYENDO },
                    recentlyAdded = allBooks.sortedByDescending { it.dateAdded }.take(10),
                    totalBooks = allBooks.size,
                    readingCount = allBooks.count { it.status == ReadingStatus.LEYENDO },
                    finishedCount = finished.size,
                    pendingCount = allBooks.count { it.status == ReadingStatus.QUIERO_LEER },
                    finishedBooks = finished,
                    insights = computeInsights(allBooks)
                )
            }
        }
    }

    private fun computeInsights(books: List<Book>): ReadingInsights {
        val finished = books.filter { it.status == ReadingStatus.TERMINADO }
        val totalPagesRead = finished.sumOf { it.pageCount ?: 0 }
        val ratings = books.mapNotNull { it.rating }
        val averageRating = if (ratings.isNotEmpty()) ratings.sum() / ratings.size else 0f

        val genreCounts = books.mapNotNull { it.genre }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .mapValues { it.value.size }
        val topGenre = genreCounts.maxByOrNull { it.value }?.key

        val calendar = Calendar.getInstance()
        val thisMonth = calendar.get(Calendar.MONTH)
        val thisYear = calendar.get(Calendar.YEAR)
        val booksThisMonth = finished.count { book ->
            book.dateFinished?.let { timestamp ->
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear
            } ?: false
        }

        return ReadingInsights(
            totalBooks = books.size,
            totalPagesRead = totalPagesRead,
            averageRating = averageRating,
            topGenre = topGenre,
            booksThisMonth = booksThisMonth
        )
    }
}
