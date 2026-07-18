package com.alexandria.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val genre: String,
    val seriesName: String? = null,
    val seriesOrder: Int? = null,
    val year: Int? = null,
    val status: String,
    val coverUrl: String? = null,
    val coverLocalPath: String? = null,
    val rating: Float? = null,
    val notes: String? = null,
    val pageCount: Int? = null,
    val isbn: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateFinished: Long? = null
)
