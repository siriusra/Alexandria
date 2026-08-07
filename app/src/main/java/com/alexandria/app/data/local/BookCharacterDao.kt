package com.alexandria.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alexandria.app.data.local.entity.BookCharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookCharacterDao {

    @Query("SELECT * FROM book_characters WHERE bookId = :bookId ORDER BY sortOrder ASC, id ASC")
    fun getForBook(bookId: Long): Flow<List<BookCharacterEntity>>

    @Query("SELECT * FROM book_characters WHERE bookId = :bookId ORDER BY sortOrder ASC, id ASC")
    suspend fun getForBookOnce(bookId: Long): List<BookCharacterEntity>

    @Query("SELECT * FROM book_characters ORDER BY bookId ASC, sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<BookCharacterEntity>

    @Insert
    suspend fun insert(character: BookCharacterEntity): Long

    @Insert
    suspend fun insertAll(characters: List<BookCharacterEntity>)

    @Update
    suspend fun update(character: BookCharacterEntity)

    @Query("UPDATE book_characters SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE book_characters SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Delete
    suspend fun delete(character: BookCharacterEntity)

    @Query("DELETE FROM book_characters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM book_characters WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
