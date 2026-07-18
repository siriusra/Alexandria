package com.alexandria.app.data.local

import androidx.room.*
import com.alexandria.app.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY dateAdded DESC")
    fun getBooksByStatus(status: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE genre = :genre ORDER BY dateAdded DESC")
    fun getBooksByGenre(genre: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE seriesName = :seriesName ORDER BY seriesOrder ASC")
    fun getBooksBySeries(seriesName: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Long): Flow<BookEntity?>

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE '%' || :query || '%' 
        OR author LIKE '%' || :query || '%'
        OR genre LIKE '%' || :query || '%'
        ORDER BY dateAdded DESC
    """)
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT DISTINCT genre FROM books ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT seriesName FROM books WHERE seriesName IS NOT NULL ORDER BY seriesName ASC")
    fun getAllSeries(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    fun getBookCountByStatus(status: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)
}
