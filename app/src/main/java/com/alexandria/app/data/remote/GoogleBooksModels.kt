package com.alexandria.app.data.remote

data class GoogleBookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val imageLinks: ImageLinks? = null,
    val categories: List<String>? = null,
    val industryIdentifiers: List<IndustryIdentifier>? = null,
    val seriesName: String? = null
)

data class ImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null
)

data class IndustryIdentifier(
    val type: String,
    val identifier: String
)

data class GoogleBooksData(
    val description: String? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null
)
