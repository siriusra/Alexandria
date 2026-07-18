package com.alexandria.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class GoogleBooksResponse(
    val totalItems: Int,
    val items: List<GoogleBookItem>?
)

data class GoogleBookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val publishedDate: String?,
    val description: String?,
    val pageCount: Int?,
    val imageLinks: ImageLinks?,
    val categories: List<String>?,
    val industryIdentifiers: List<IndustryIdentifier>?
)

data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)

data class IndustryIdentifier(
    val type: String,
    val identifier: String
)

interface GoogleBooksApi {

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("startIndex") startIndex: Int = 0,
        @Query("printType") printType: String = "books"
    ): GoogleBooksResponse

    @GET("volumes/{volumeId}")
    suspend fun getBookById(
        @Path("volumeId") volumeId: String
    ): GoogleBookItem
}
