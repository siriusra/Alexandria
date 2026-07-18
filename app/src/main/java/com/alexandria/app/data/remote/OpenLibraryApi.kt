package com.alexandria.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class OpenLibraryResponse(
    val numFound: Int,
    val docs: List<OpenLibraryDoc>
)

data class OpenLibraryDoc(
    val title: String?,
    val author_name: List<String>?,
    val cover_i: Long?,
    val first_publish_year: Int?,
    val isbn: List<String>?,
    val key: String?
)

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = "key,title,author_name,cover_i,first_publish_year,isbn",
        @Query("limit") limit: Int = 20
    ): OpenLibraryResponse
}
