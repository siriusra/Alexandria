package com.alexandria.app.data.repository

import android.util.Log
import com.alexandria.app.data.local.BookCharacterDao
import com.alexandria.app.data.local.BookDao
import com.alexandria.app.data.local.CoverStore
import com.alexandria.app.data.local.MetadataCacheDao
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.local.entity.BookCharacterEntity
import com.alexandria.app.data.local.entity.BookEntity
import com.alexandria.app.data.local.entity.MetadataCacheEntity
import com.alexandria.app.data.remote.GoogleBookItem
import com.alexandria.app.data.remote.PortadaResolver
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.BookCharacter
import com.alexandria.app.domain.model.ReadingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val bookCharacterDao: BookCharacterDao,
    private val portadaResolver: PortadaResolver,
    private val coverStore: CoverStore,
    private val preferencesManager: PreferencesManager,
    private val metadataCacheDao: MetadataCacheDao
) {
    fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>> {
        return bookDao.getBooksByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBooksByGenre(genre: String): Flow<List<Book>> {
        return bookDao.getBooksByGenre(genre).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBooksBySeries(seriesName: String): Flow<List<Book>> {
        return bookDao.getBooksBySeries(seriesName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBookById(bookId: Long): Flow<Book?> {
        return bookDao.getBookById(bookId).map { it?.toDomain() }
    }

    fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllGenres(): Flow<List<String>> = bookDao.getAllGenres()

    fun getAllSeries(): Flow<List<String>> = bookDao.getAllSeries()

    fun getBookCount(): Flow<Int> = bookDao.getBookCount()

    fun getBookCountByStatus(status: ReadingStatus): Flow<Int> {
        return bookDao.getBookCountByStatus(status.name)
    }

    suspend fun addBook(book: Book): Long {
        val id = bookDao.insertBook(book.toEntity())
        autoResolveCover(id, book)
        return id
    }

    suspend fun updateCurrentPage(bookId: Long, currentPage: Int) {
        bookDao.updateCurrentPage(bookId, currentPage)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book.toEntity())
        if (book.coverUrl == null) {
            autoResolveCover(book.id, book)
        }
    }

    suspend fun updateBookDescription(bookId: Long, description: String?) {
        bookDao.updateDescription(bookId, description)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book.toEntity())
    }

    suspend fun deleteBookById(bookId: Long) {
        bookDao.deleteBookById(bookId)
        bookCharacterDao.deleteForBook(bookId)
    }

    // ===== CHARACTERS =====

    fun getCharactersForBook(bookId: Long): Flow<List<BookCharacter>> {
        return bookCharacterDao.getForBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addCharacter(character: BookCharacter): Long {
        return bookCharacterDao.insert(character.toEntity())
    }

    suspend fun addCharacters(characters: List<BookCharacter>) {
        if (characters.isNotEmpty()) {
            bookCharacterDao.insertAll(characters.map { it.toEntity() })
        }
    }

    suspend fun updateCharacter(character: BookCharacter) {
        bookCharacterDao.update(character.toEntity())
    }
    suspend fun updateCharacterFavorite(id: Long, isFavorite: Boolean) {
        bookCharacterDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteCharacter(character: BookCharacter) {
        bookCharacterDao.delete(character.toEntity())
    }

    suspend fun deleteCharacterById(id: Long) {
        bookCharacterDao.deleteById(id)
    }

    // ===== BACKUP =====

    suspend fun getAllBooksOnce(): List<Book> {
        return bookDao.getAllBooks().first().map { it.toDomain() }
    }

    suspend fun getAllCharactersOnce(): List<BookCharacter> {
        return bookCharacterDao.getAllOnce().map { it.toDomain() }
    }

    suspend fun readCoverBase64(localPath: String?): String? {
        return coverStore.readCoverBytes(localPath)?.let { bytes ->
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    /** Restaura un libro (genera ID nuevo), sus personajes y la portada embebida. */
    suspend fun restoreBook(
        book: Book,
        characters: List<BookCharacter>,
        coverBytes: ByteArray?
    ): Long {
        val newId = bookDao.insertBook(
            book.copy(id = 0, coverLocalPath = null).toEntity()
        )
        if (coverBytes != null) {
            val localPath = coverStore.restoreCoverBytes(book.isbn, book.coverUrl, coverBytes)
            if (localPath != null) {
                bookDao.updateCoverLocalPath(newId, localPath)
            }
        }
        if (characters.isNotEmpty()) {
            bookCharacterDao.insertAll(
                characters.map { it.copy(id = 0, bookId = newId).toEntity() }
            )
        }
        return newId
    }


    suspend fun searchCovers(query: String): List<GoogleBookItem> {
        return try {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) return emptyList()

            val openLibraryResults = portadaResolver.buscarCoversOpenLibrary(trimmedQuery)
            val googleBooksResults = portadaResolver.buscarCoversGoogleBooks(trimmedQuery)

            val seenIds = mutableSetOf<String>()
            val merged = mutableListOf<GoogleBookItem>()

            for (item in openLibraryResults) {
                merged.add(item)
                seenIds.add(item.id)
            }
            for (item in googleBooksResults) {
                if (item.id !in seenIds) {
                    merged.add(item)
                    seenIds.add(item.id)
                }
            }

            merged
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("BookRepository", "Error searching covers for: $query", e)
            emptyList()
        }
    }

    private suspend fun autoResolveCover(bookId: Long, book: Book) {
        try {
            val url = portadaResolver.resolver(
                isbn = book.isbn,
                titulo = book.title,
                autor = book.author
            )
            if (url != null) {
                bookDao.updateCoverUrl(bookId, url)
                persistCoverLocal(bookId, book, url)
                Log.d(TAG, "Cover resolved for '${book.title}': $url")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving cover for '${book.title}'", e)
        }
    }

    private suspend fun persistCoverLocal(bookId: Long, book: Book, coverUrl: String) {
        try {
            val downloadEnabled = preferencesManager.coverDownloadEnabled.first()
            if (!downloadEnabled) return
            val localPath = coverStore.saveCover(isbn = book.isbn, url = coverUrl)
            if (localPath != null) {
                bookDao.updateCoverLocalPath(bookId, localPath)
                Log.d(TAG, "Cover saved locally for book $bookId: $localPath")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cover download deferred/failed for '${book.title}'", e)
        }
    }

    suspend fun ensureCoverPersisted(bookId: Long, book: Book, coverUrl: String) {
        if (book.coverLocalPath.isNullOrBlank()) {
            persistCoverLocal(bookId, book, coverUrl)
        }
    }

    suspend fun persistCover(bookId: Long, book: Book, coverUrl: String) {
        if (!book.coverUrl.isNullOrBlank()) return
        bookDao.updateCoverUrl(bookId, coverUrl)
        persistCoverLocal(bookId, book, coverUrl)
    }

    suspend fun getCachedMetadata(isbn: String): MetadataCacheEntity? {
        if (isbn.isBlank()) return null
        return try {
            val cached = metadataCacheDao.get(isbn) ?: return null
            val expired = System.currentTimeMillis() - cached.timestamp >= cached.ttlMs
            if (expired) {
                metadataCacheDao.evictExpired(System.currentTimeMillis())
                null
            } else {
                cached
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "metadata cache read failed for $isbn", e)
            null
        }
    }

    suspend fun cacheMetadata(
        isbn: String,
        description: String?,
        averageRating: Float?,
        ratingsCount: Int?,
        source: String
    ) {
        if (isbn.isBlank()) return
        try {
            val ttlMs = 7L * 24 * 60 * 60 * 1000
            metadataCacheDao.put(
                MetadataCacheEntity(
                    isbn = isbn,
                    description = description,
                    averageRating = averageRating,
                    ratingsCount = ratingsCount,
                    source = source,
                    timestamp = System.currentTimeMillis(),
                    ttlMs = ttlMs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "metadata cache write failed for $isbn", e)
        }
    }

    private fun BookEntity.toDomain(): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            genre = genre,
            seriesName = seriesName,
            seriesOrder = seriesOrder,
            year = year,
            status = ReadingStatus.fromString(status),
            coverUrl = coverUrl,
            coverLocalPath = coverLocalPath,
            rating = rating,
            notes = notes,
            description = description,
            pageCount = pageCount,
            currentPage = currentPage,
            isbn = isbn,
            dateAdded = dateAdded,
            dateFinished = dateFinished
        )
    }

    private fun BookCharacterEntity.toDomain(): BookCharacter {
        return BookCharacter(
            id = id,
            bookId = bookId,
            name = name,
            iconType = iconType,
            iconKey = iconKey,
            isFavorite = isFavorite,
            sortOrder = sortOrder
        )
    }

    private fun BookCharacter.toEntity(): BookCharacterEntity {
        return BookCharacterEntity(
            id = id,
            bookId = bookId,
            name = name,
            iconType = iconType,
            iconKey = iconKey,
            isFavorite = isFavorite,
            sortOrder = sortOrder
        )
    }

    private fun Book.toEntity(): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            author = author,
            genre = genre,
            seriesName = seriesName,
            seriesOrder = seriesOrder,
            year = year,
            status = status.name,
            coverUrl = coverUrl,
            coverLocalPath = coverLocalPath,
            rating = rating,
            notes = notes,
            description = description,
            pageCount = pageCount,
            currentPage = currentPage,
            isbn = isbn,
            dateAdded = dateAdded,
            dateFinished = dateFinished
        )
    }

    companion object {
        private const val TAG = "BookRepository"
    }
}
