package com.alexandria.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.minOf
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

        val gbResult = runCatching { googleBooksCover(cleanIsbn, titulo, autor) }.getOrNull()
        if (gbResult != null) {
            Log.d(TAG, "Google Books cover found for '$titulo'")
            return@withContext gbResult
        }

        Log.d(TAG, "No cover found for '$titulo'")
        null
    }

    suspend fun buscarGoogleBooks(query: String, maxResults: Int = 20): List<GoogleBookItem> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/books/v1/volumes?q=$query&maxResults=$maxResults"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            if (!json.has("items")) return@withContext emptyList()
            val items = json.getJSONArray("items")
            val results = mutableListOf<GoogleBookItem>()
            for (i in 0 until minOf(items.length(), maxResults)) {
                val item = items.getJSONObject(i)
                val id = item.optString("id", "gb_$i")
                val vi = item.optJSONObject("volumeInfo") ?: continue
                val title = vi.optString("title", "")
                if (title.isBlank()) continue
                val imgLinks = vi.optJSONObject("imageLinks")
                var thumbnail: String? = null
                var smallThumbnail: String? = null
                if (imgLinks != null) {
                    thumbnail = imgLinks.optString("thumbnail", "").takeIf { it.isNotBlank() }
                    smallThumbnail = imgLinks.optString("smallThumbnail", "").takeIf { it.isNotBlank() }
                }
                val authors = vi.optJSONArray("authors")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }
                results.add(GoogleBookItem(
                    id = id,
                    volumeInfo = VolumeInfo(
                        title = title,
                        authors = authors,
                        publishedDate = vi.optString("publishedDate", "").takeIf { it.isNotBlank() },
                        description = vi.optString("description", "").takeIf { it.isNotBlank() },
                        pageCount = vi.optInt("pageCount", 0).takeIf { it > 0 },
                        imageLinks = ImageLinks(
                            smallThumbnail = smallThumbnail?.replace("http://", "https://"),
                            thumbnail = thumbnail?.replace("http://", "https://")
                        ),
                        categories = null,
                        industryIdentifiers = null
                    )
                ))
            }
            Log.d(TAG, "Google Books search: found ${results.size} results for '$query'")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Google Books", e)
            emptyList()
        }
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

    private fun googleBooksCover(isbn: String?, titulo: String, autor: String?): String? {
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
