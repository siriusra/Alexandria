package com.alexandria.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexandria.app.data.local.entity.CoverCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoverCacheDao {
    @Query("SELECT * FROM cover_cache WHERE isbn = :isbn")
    suspend fun get(isbn: String): CoverCacheEntity?

    @Query("SELECT * FROM cover_cache WHERE isbn = :isbn")
    fun getFlow(isbn: String): Flow<CoverCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CoverCacheEntity)

    @Query("DELETE FROM cover_cache WHERE timestamp < :cutoff")
    suspend fun evictOld(cutoff: Long)

    @Query("DELETE FROM cover_cache WHERE isbn = :isbn")
    suspend fun delete(isbn: String)
}