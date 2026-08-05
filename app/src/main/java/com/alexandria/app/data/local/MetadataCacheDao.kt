package com.alexandria.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexandria.app.data.local.entity.MetadataCacheEntity

@Dao
interface MetadataCacheDao {

    @Query("SELECT * FROM metadata_cache WHERE isbn = :isbn")
    suspend fun get(isbn: String): MetadataCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: MetadataCacheEntity)

    @Query("DELETE FROM metadata_cache WHERE timestamp < :nowMs")
    suspend fun evictExpired(nowMs: Long): Int
}
