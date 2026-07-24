package com.alexandria.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PortadaResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun resolver(isbn: String?, titulo: String, autor: String?): String? = withContext(Dispatchers.IO) {
        val cleanIsbn = isbn?.replace(Regex("[\\s-]"), "")

        val olResult = runCatching { openLibrary(cleanIsbn) }.getOrNull()
        if (olResult != null) {
            Log.d(TAG, "Open Library cover found for ISBN $cleanIsbn")
            return@withContext olResult
        }

        val gbResult = runCatching { googleBooks(cleanIsbn, titulo, autor) }.getOrNull()
        if (gbResult != null) {
            Log.d(TAG, "Google Books cover found for '$titulo'")
            return@withContext gbResult
        }

        Log.d(TAG, "No cover found for '$titulo'")
        null
    }

    private fun openLibrary(isbn: String?): String? {
        if (isbn.isNullOrBlank()) return null

        val url = "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            val contentType = response.header("Content-Type", "")
            if (contentType?.contains("image") == true) url else null
        } else {
            null
        }
    }

    private fun googleBooks(isbn: String?, titulo: String, autor: String?): String? {
        val query = when {
            !isbn.isNullOrBlank() -> "isbn:$isbn"
            else -> {
                val titlePart = "intitle:\"$titulo\""
                val authorPart = if (!autor.isNullOrBlank()) "+inauthor:\"$autor\"" else ""
                "$titlePart$authorPart"
            }
        }

        val url = "https://www.googleapis.com/books/v1/volumes?q=$query&maxResults=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        if (!json.has("items")) return null

        val items = json.getJSONArray("items")
        if (items.length() == 0) return null

        val volumeInfo = items.getJSONObject(0).optJSONObject("volumeInfo") ?: return null
        val imageLinks = volumeInfo.optJSONObject("imageLinks") ?: return null

        var thumbnail = imageLinks.optString("thumbnail", "")
        if (thumbnail.isBlank()) {
            thumbnail = imageLinks.optString("smallThumbnail", "")
        }
        if (thumbnail.isBlank()) return null

        thumbnail = thumbnail.replace("http://", "https://")
        thumbnail = thumbnail.replace(Regex("&edge=curl"), "")

        return thumbnail
    }

    companion object {
        private const val TAG = "PortadaResolver"
    }
}
