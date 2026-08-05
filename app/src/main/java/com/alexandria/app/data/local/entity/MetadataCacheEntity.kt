package com.alexandria.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(
    @PrimaryKey
    val isbn: String,
    val description: String?,
    val averageRating: Float?,
    val ratingsCount: Int?,
    val source: String,
    val timestamp: Long,
    val ttlMs: Long
)
