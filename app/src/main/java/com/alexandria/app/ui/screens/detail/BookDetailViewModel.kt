package com.alexandria.app.ui.screens.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.model.CoverSourceConfig
import com.alexandria.app.data.remote.CloudBookMetadata
import com.alexandria.app.data.remote.CloudResolver
import com.alexandria.app.data.remote.PortadaResolver
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.BookCharacter
import com.alexandria.app.domain.model.ReadingStatus
import com.alexandria.app.data.repository.BookRepository
import com.alexandria.app.ui.components.ICON_TYPE_EMOJI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
    private val coverCacheDao: CoverCacheDao,
    private val cloudResolver: CloudResolver,
    private val auth: FirebaseAuth
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
    private var cloudAttempted = false
    private var topicSubscribed = false

    private suspend fun currentUid(): String? {
        return try {
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            user?.uid?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun subscribeToUserTopic(uid: String) {
        if (topicSubscribed) return
        topicSubscribed = true
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("user-$uid").await()
        } catch (e: Exception) {
            // Topic subscription is best-effort; cloud resolution still works on demand.
        }
    }

    private suspend fun tryCloudResolve(book: Book) {
        if (cloudAttempted) return
        val uid = currentUid() ?: return
        subscribeToUserTopic(uid)
        cloudAttempted = true
        val metadata = try {
            withTimeout(15_000L) { cloudResolver.resolveBook(uid, book) }
        } catch (e: Exception) {
            null
        } ?: return
        val current = _uiState.value
        var updated = current

        if (!metadata.coverUrl.isNullOrBlank()) {
            repository.persistCover(book.id, book, metadata.coverUrl)
            updated = updated.copy(coverUrl = metadata.coverUrl)
        }
        if (!metadata.description.isNullOrBlank() && metadata.description != book.description) {
            repository.updateBookDescription(book.id, metadata.description)
            updated = updated.copy(description = metadata.description)
        }
        if (metadata.averageRating != null) {
            updated = updated.copy(
                externalRating = metadata.averageRating,
                externalRatingsCount = metadata.ratingsCount,
                ratingSource = metadata.ratingSource ?: "cloud"
            )
        }
        if (metadata.characters.isNotEmpty() && updated.characters.isEmpty()) {
            val chars = metadata.characters.map { c ->
                BookCharacter(
                    bookId = book.id,
                    name = c.name,
                    isFavorite = c.isFavorite,
                    iconType = ICON_TYPE_EMOJI,
                    iconKey = c.emoji?.ifBlank { null } ?: ""
                )
            }
            repository.addCharacters(chars)
            updated = updated.copy(characters = updated.characters + chars)
        }
        _uiState.value = updated
    }

    private fun loadBook() {
        if (bookId == 0L) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            var hasReceivedBook = false
            var timeoutJob: Job? = null
            repository.getBookById(bookId).collect { book ->
                val current = _uiState.value
                val effective = book ?: current.book
                val gotBookNow = effective != null
                if (gotBookNow && !hasReceivedBook) {
                    hasReceivedBook = true
                    timeoutJob?.cancel()
                    timeoutJob = null
                }
                _uiState.value = current.copy(
                    book = effective,
                    isLoading = !gotBookNow,
                    description = effective?.description ?: current.description,
                    coverUrl = effective?.coverUrl ?: effective?.coverLocalPath
                )
                if (gotBookNow) {
                    if (!descriptionFetched) {
                        descriptionFetched = true
                        fetchDescription(effective!!)
                    }
                    if (!coverFetched) {
                        coverFetched = true
                        fetchCover(effective!!)
                    }
                } else if (!hasReceivedBook && timeoutJob == null) {
                    timeoutJob = viewModelScope.launch {
                        delay(3000)
                        val current2 = _uiState.value
                        if (current2.book == null && current2.isLoading) {
                            _uiState.value = current2.copy(isLoading = false)
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchCover(book: Book) {
        if (!book.coverUrl.isNullOrBlank() || !book.coverLocalPath.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(coverUrl = book.coverUrl ?: book.coverLocalPath)
            return
        }
        tryCloudResolve(book)
        if (_uiState.value.coverUrl != null) return
        val config = preferencesManager.coverSourcesConfig.first()
        val coverUrl = portadaResolver.resolverCover(
            isbn = book.isbn,
            titulo = book.title,
            autor = book.author,
            coverCacheDao = coverCacheDao,
            config = config
        )
        if (coverUrl != null) {
            repository.persistCover(book.id, book, coverUrl)
            _uiState.value = _uiState.value.copy(coverUrl = coverUrl)
        }
    }

    private suspend fun fetchDescription(book: Book) {
        _uiState.value = _uiState.value.copy(isDescriptionLoading = true)
        var desc: String? = null
        var externalRating: Double? = null
        var externalRatingsCount: Int? = null
        var ratingSource: String? = null
        try {
            // Cloud-first: if the cloud resolve already filled description/rating, skip the local chain.
            tryCloudResolve(book)
            val cloudState = _uiState.value
            if (!cloudState.description.isNullOrBlank() && cloudState.description != book.description) {
                repository.updateBookDescription(book.id, cloudState.description)
            }
            if (!cloudState.description.isNullOrBlank()) {
                return
            }

            val sources = preferencesManager.synopsisSources.first()

            // Read-through: skip the network when a fresh metadata cache entry exists.
            if (!book.isbn.isNullOrBlank()) {
                val cached = repository.getCachedMetadata(book.isbn)
                if (cached != null && !cached.description.isNullOrBlank()) {
                    desc = cached.description
                    externalRating = cached.averageRating?.toDouble()
                    externalRatingsCount = cached.ratingsCount
                    ratingSource = cached.source
                    _uiState.value = _uiState.value.copy(
                        description = desc,
                        externalRating = externalRating,
                        externalRatingsCount = externalRatingsCount,
                        ratingSource = ratingSource
                    )
                    if (desc != book.description) repository.updateBookDescription(book.id, desc)
                    return
                }
            }

            for (key in sources.enabledSources) {
                when (key) {
                    "isbn" -> if (!book.isbn.isNullOrBlank()) {
                        val candidate = timed {
                            portadaResolver.fetchDescriptionFromIsbn(book.isbn)
                        }
                        if (desc == null && candidate != null && portadaResolver.isSpanishText(candidate)) {
                            desc = candidate
                        }
                    }
                    "bne" -> if (desc == null && !book.isbn.isNullOrBlank()) {
                        val candidate = timed {
                            portadaResolver.fetchDescriptionFromBne(book.isbn)
                        }
                        if (candidate != null && portadaResolver.isSpanishText(candidate)) {
                            desc = candidate
                        }
                    }
                    "openlibrary" -> {
                        val olData = timed {
                            portadaResolver.fetchDescriptionBySearch(book.title, book.author, lang = "spa")
                        }
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
                        val candidate = timed {
                            portadaResolver.fetchDescriptionFromWikipedia(book.title, book.author)
                        }
                        if (candidate != null && portadaResolver.isSpanishText(candidate)) {
                            desc = candidate
                        }
                    }
                    "google_books" -> {
                        val googleData = timed {
                            portadaResolver.fetchFromGoogleBooks(book.title, book.author, book.isbn)
                        }
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
                description = desc ?: _uiState.value.description,
                externalRating = externalRating,
                externalRatingsCount = externalRatingsCount,
                ratingSource = ratingSource
            )
            if (desc != null && desc != book.description) {
                repository.updateBookDescription(book.id, desc)
            }

            // Write-through: cache the resolved metadata keyed by ISBN.
            if (!book.isbn.isNullOrBlank() && desc != null) {
                repository.cacheMetadata(
                    isbn = book.isbn,
                    description = desc,
                    averageRating = externalRating?.toFloat(),
                    ratingsCount = externalRatingsCount,
                    source = ratingSource ?: "mixed"
                )
            }
        } finally {
            _uiState.value = _uiState.value.copy(isDescriptionLoading = false)
        }
    }

    private suspend fun <T> timed(block: suspend () -> T): T? {
        return try {
            withTimeout(15_000) { block() }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
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