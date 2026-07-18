package com.alexandria.app.data.repository

import com.alexandria.app.data.local.BookDao
import com.alexandria.app.data.local.entity.BookEntity
import com.alexandria.app.data.remote.CoverService
import com.alexandria.app.data.remote.GoogleBookItem
import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val coverService: CoverService
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
        return bookDao.insertBook(book.toEntity())
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book.toEntity())
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book.toEntity())
    }

    suspend fun deleteBookById(bookId: Long) {
        bookDao.deleteBookById(bookId)
    }

    suspend fun searchCovers(query: String): List<GoogleBookItem> {
        return try {
            val response = coverService.googleBooksApi.searchBooks(query)
            response.items ?: emptyList()
        } catch (e: Exception) {
            emptyList()
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
            pageCount = pageCount,
            isbn = isbn,
            dateAdded = dateAdded,
            dateFinished = dateFinished
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
            pageCount = pageCount,
            isbn = isbn,
            dateAdded = dateAdded,
            dateFinished = dateFinished
        )
    }
}
