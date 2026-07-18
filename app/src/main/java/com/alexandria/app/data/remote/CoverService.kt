package com.alexandria.app.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverService @Inject constructor() {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val googleBooksRetrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/books/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val openLibraryRetrofit = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val googleBooksApi: GoogleBooksApi = googleBooksRetrofit.create(GoogleBooksApi::class.java)

    val openLibraryApi: OpenLibraryApi = openLibraryRetrofit.create(OpenLibraryApi::class.java)

    fun getCoverUrl(volumeInfo: VolumeInfo): String? {
        return volumeInfo.imageLinks?.thumbnail
            ?.replace("http://", "https://")
            ?.replace("edge=curl", "edge=curl&fife=w400-h600")
    }

    fun getOpenLibraryCoverUrl(coverId: Long): String {
        return "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
    }
}
