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

    fun getCoverUrl(volumeInfo: VolumeInfo, volumeId: String?): String? {
        val thumbnail = volumeInfo.imageLinks?.thumbnail
        if (thumbnail != null) {
            val sanitized = sanitizeGoogleCoverUrl(thumbnail)
            if (sanitized != null) return sanitized
        }
        return buildGoogleBooksCoverUrl(volumeId)
    }

    fun buildGoogleBooksCoverUrl(volumeId: String?, zoom: Int = 2): String? {
        if (volumeId.isNullOrBlank()) return null
        return "https://books.google.com/books/content?id=$volumeId" +
                "&printsec=frontcover&img=1&zoom=$zoom&edge=curl&source=gbs_api"
    }

    fun sanitizeGoogleCoverUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        var sanitized = url.replace("http://", "https://")
        sanitized = sanitized.replace(Regex("[&?]imgtk=[^&]*"), "")
        sanitized = sanitized.replace("&&+", "&")
        sanitized = sanitized.replace("?&", "?")
        sanitized = sanitized.trimEnd('&', '?')
        return sanitized.ifBlank { null }
    }

    fun getOpenLibraryCoverUrl(coverId: Long): String {
        return "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
    }
}
