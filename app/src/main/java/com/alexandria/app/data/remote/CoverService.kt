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
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/books/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val googleBooksApi: GoogleBooksApi = retrofit.create(GoogleBooksApi::class.java)

    fun getCoverUrl(volumeInfo: VolumeInfo): String? {
        return volumeInfo.imageLinks?.thumbnail
            ?.replace("http://", "https://")
            ?.replace("edge=curl", "edge=curl&fife=w400-h600")
    }
}
