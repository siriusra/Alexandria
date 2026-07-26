package com.alexandria.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.remote.PortadaResolver
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val book: Book? = null,
    val isLoading: Boolean = true,
    val description: String? = null,
    val isDescriptionLoading: Boolean = false,
    val externalRating: Double? = null,
    val externalRatingsCount: Int? = null,
    val ratingSource: String? = null
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
    private val portadaResolver: PortadaResolver,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
    }

    private var descriptionFetched = false

    private fun loadBook() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _uiState.value = DetailUiState(
                    book = book,
                    isLoading = false
                )
                if (book != null && !descriptionFetched) {
                    descriptionFetched = true
                    fetchDescription(book)
                }
            }
        }
    }

    private suspend fun fetchDescription(book: Book) {
        _uiState.value = _uiState.value.copy(isDescriptionLoading = true)
        var desc: String? = null
        var externalRating: Double? = null
        var externalRatingsCount: Int? = null
        var ratingSource: String? = null
        val sources = preferencesManager.synopsisSources.first()

        if (sources.isbn && !book.isbn.isNullOrBlank()) {
            desc = portadaResolver.fetchDescriptionFromIsbn(book.isbn)
        }

        if (desc == null && sources.todostuslibros && !book.isbn.isNullOrBlank()) {
            desc = portadaResolver.fetchDescriptionFromTodoTusLibros(book.isbn)
        }

        if (desc == null && sources.casaDelLibro) {
            desc = portadaResolver.fetchDescriptionFromCasaDelLibro(book.title, book.author)
        }

        if (sources.openLibrary) {
            val olData = portadaResolver.fetchDescriptionBySearch(book.title, book.author, lang = "spa")
            if (olData != null) {
                if (desc == null && olData.description != null) desc = olData.description
                if (externalRating == null && olData.averageRating != null) {
                    externalRating = olData.averageRating
                    externalRatingsCount = olData.ratingsCount
                    ratingSource = "OpenLibrary"
                }
            }
        }

        if (desc == null && sources.wikipedia) {
            desc = portadaResolver.fetchDescriptionFromWikipedia(book.title, book.author)
        }

        if (sources.googleBooks) {
            val googleData = portadaResolver.fetchFromGoogleBooks(book.title, book.author)
            if (googleData != null) {
                if (desc == null && googleData.description != null) desc = googleData.description
                if (externalRating == null && googleData.averageRating != null) {
                    externalRating = googleData.averageRating
                    externalRatingsCount = googleData.ratingsCount
                    ratingSource = "Google Books"
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            description = desc,
            isDescriptionLoading = false,
            externalRating = externalRating,
            externalRatingsCount = externalRatingsCount,
            ratingSource = ratingSource
        )
    }

    fun updateCurrentPage(page: Int) {
        viewModelScope.launch {
            repository.updateCurrentPage(bookId, page)
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
