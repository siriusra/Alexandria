package com.alexandria.app.domain.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val genre: String,
    val seriesName: String? = null,
    val seriesOrder: Int? = null,
    val year: Int? = null,
    val status: ReadingStatus = ReadingStatus.PENDING,
    val coverUrl: String? = null,
    val coverLocalPath: String? = null,
    val rating: Float? = null,
    val notes: String? = null,
    val pageCount: Int? = null,
    val isbn: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateFinished: Long? = null
)
