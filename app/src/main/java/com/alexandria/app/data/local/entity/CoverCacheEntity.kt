package com.alexandria.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cover_cache")
data class CoverCacheEntity(
    @PrimaryKey val isbn: String,
    val coverUrl: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)