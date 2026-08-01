package com.alexandria.app.ui.screens.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.model.CoverSourceConfig
import com.alexandria.app.data.remote.PortadaResolver
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.BookCharacter
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import com.alexandria.app.ui.components.ICON_TYPE_EMOJI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val ratingSource: String? = null,
    val coverUrl: String? = null,
    val characters: List<BookCharacter> = emptyList(),
    val isCharactersLoading: Boolean = false,
    val characterSuggestions: List<String>? = null
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
    private val portadaResolver: PortadaResolver,
    private val preferencesManager: PreferencesManager,
    private val coverCacheDao: CoverCacheDao
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        viewModelScope.launch {
            repository.getCharactersForBook(bookId).collect { characters ->
                _uiState.value = _uiState.value.copy(characters = characters)
            }
        }
    }

    private var descriptionFetched = false
    private var coverFetched = false

    private fun loadBook() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _uiState.value = _uiState.value.copy(
                    book = book,
                    isLoading = false,
                    coverUrl = book?.coverUrl ?: book?.coverLocalPath
                )
                if (book != null && !descriptionFetched) {
                    descriptionFetched = true
                    fetchDescription(book)
                }
                if (book != null && !coverFetched) {
                    coverFetched = true
                    fetchCover(book)
                }
            }
        }
    }

    private suspend fun fetchCover(book: Book) {
        if (!book.coverUrl.isNullOrBlank() || !book.coverLocalPath.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(coverUrl = book.coverUrl ?: book.coverLocalPath)
            return
        }
        val config = preferencesManager.coverSourcesConfig.first()
        val coverUrl = portadaResolver.resolverCover(
            isbn = book.isbn,
            titulo = book.title,
            autor = book.author,
            coverCacheDao = coverCacheDao,
            config = config
        )
        if (coverUrl != null) {
            _uiState.value = _uiState.value.copy(coverUrl = coverUrl)
        }
    }

    private suspend fun fetchDescription(book: Book) {
        _uiState.value = _uiState.value.copy(isDescriptionLoading = true)
        var desc: String? = null
        var externalRating: Double? = null
        var externalRatingsCount: Int? = null
        var ratingSource: String? = null
        val sources = preferencesManager.synopsisSources.first()

        for (key in sources.enabledSources) {
            when (key) {
                "isbn" -> if (!book.isbn.isNullOrBlank()) {
                    val candidate = portadaResolver.fetchDescriptionFromIsbn(book.isbn)
                    if (desc == null && candidate != null && portadaResolver.isSpanishText(candidate)) {
                        desc = candidate
                    }
                }
                "todostuslibros" -> if (desc == null && !book.isbn.isNullOrBlank()) {
                    val candidate = portadaResolver.fetchDescriptionFromTodoTusLibros(book.isbn)
                    if (candidate != null && portadaResolver.isSpanishText(candidate)) {
                        desc = candidate
                    }
                }
                "casa_del_libro" -> if (desc == null) {
                    val candidate = portadaResolver.fetchDescriptionFromCasaDelLibro(book.title, book.author)
                    if (candidate != null && portadaResolver.isSpanishText(candidate)) {
                        desc = candidate
                    }
                }
                "openlibrary" -> {
                    val olData = portadaResolver.fetchDescriptionBySearch(book.title, book.author, lang = "spa")
                    if (olData != null) {
                        if (desc == null && olData.description != null && portadaResolver.isSpanishText(olData.description)) {
                            desc = olData.description
                        }
                        if (externalRating == null && olData.averageRating != null) {
                            externalRating = olData.averageRating
                            externalRatingsCount = olData.ratingsCount
                            ratingSource = "OpenLibrary"
                        }
                    }
                }
                "wikipedia" -> if (desc == null) {
                    val candidate = portadaResolver.fetchDescriptionFromWikipedia(book.title, book.author)
                    if (candidate != null && portadaResolver.isSpanishText(candidate)) {
                        desc = candidate
                    }
                }
                "google_books" -> {
                    val googleData = portadaResolver.fetchFromGoogleBooks(book.title, book.author)
                    if (googleData != null) {
                        if (desc == null && googleData.description != null && portadaResolver.isSpanishText(googleData.description)) {
                            desc = googleData.description
                        }
                        if (externalRating == null && googleData.averageRating != null) {
                            externalRating = googleData.averageRating
                            externalRatingsCount = googleData.ratingsCount
                            ratingSource = "Google Books"
                        }
                    }
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
                    dateFinished = if (newStatus == ReadingStatus.TERMINADO) {
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

    fun addCharacter(name: String, iconType: String, iconKey: String) {
        viewModelScope.launch {
            repository.addCharacter(
                BookCharacter(
                    bookId = bookId,
                    name = name,
                    iconType = iconType,
                    iconKey = iconKey,
                    sortOrder = _uiState.value.characters.size
                )
            )
        }
    }

    fun addCharacters(pairs: List<Pair<String, String>>) {
        if (pairs.isEmpty()) return
        viewModelScope.launch {
            val startOrder = _uiState.value.characters.size
            repository.addCharacters(
                pairs.mapIndexed { index, (name, iconKey) ->
                    BookCharacter(
                        bookId = bookId,
                        name = name,
                        iconType = ICON_TYPE_EMOJI,
                        iconKey = iconKey,
                        sortOrder = startOrder + index
                    )
                }
            )
        }
    }

    fun updateCharacter(id: Long, name: String, iconType: String, iconKey: String) {
        viewModelScope.launch {
            _uiState.value.characters.find { it.id == id }?.let { character ->
                repository.updateCharacter(
                    character.copy(name = name, iconType = iconType, iconKey = iconKey)
                )
            }
        }
    }

    fun toggleCharacterFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateCharacterFavorite(id, isFavorite)
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            repository.deleteCharacterById(id)
        }
    }

    fun dismissCharacterSuggestions() {
        _uiState.value = _uiState.value.copy(characterSuggestions = null)
    }

    fun searchCharacters() {
        val book = _uiState.value.book ?: return
        if (_uiState.value.isCharactersLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCharactersLoading = true)
            var found: List<String> = emptyList()
            try {
                found = portadaResolver.fetchCharacters(book.title, book.author)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error searching characters", e)
            } finally {
                _uiState.value = _uiState.value.copy(isCharactersLoading = false)
            }
            _uiState.value = _uiState.value.copy(characterSuggestions = found)
        }
    }

    private companion object {
        const val TAG = "BookDetailViewModel"
    }
}