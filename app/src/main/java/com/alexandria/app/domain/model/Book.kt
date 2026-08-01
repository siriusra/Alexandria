package com.alexandria.app.domain.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val genre: String? = null,
    val seriesName: String? = null,
    val seriesOrder: Int? = null,
    val year: Int? = null,
    val status: ReadingStatus = ReadingStatus.QUIERO_LEER,
    val coverUrl: String? = null,
    val coverLocalPath: String? = null,
    val rating: Float? = null,
    val notes: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val currentPage: Int = 0,
    val isbn: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateFinished: Long? = null
) {
    fun withDefaults(): Book = copy(
        status = status ?: ReadingStatus.QUIERO_LEER,
        genre = genre ?: "",
        coverUrl = coverUrl,
        coverLocalPath = coverLocalPath,
        rating = rating,
        notes = notes,
        pageCount = pageCount,
        currentPage = currentPage ?: 0,
        isbn = isbn,
        dateAdded = dateAdded,
        dateFinished = dateFinished
    )
}
