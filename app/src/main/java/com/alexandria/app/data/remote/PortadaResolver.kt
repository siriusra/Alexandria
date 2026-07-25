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
        .followRedirects(false)
        .build()

    suspend fun resolver(isbn: String?, titulo: String, autor: String?): String? = withContext(Dispatchers.IO) {
        val cleanIsbn = isbn?.replace(Regex("[\\s-]"), "")

        val olResult = runCatching { openLibraryCover(cleanIsbn) }.getOrNull()
        if (olResult != null) {
            Log.d(TAG, "Open Library cover found for ISBN $cleanIsbn")
            return@withContext olResult
        }

        Log.d(TAG, "No cover found for '$titulo'")
        null
    }

    suspend fun buscarCoversOpenLibrary(query: String, maxResults: Int = 20): List<GoogleBookItem> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$encodedQuery&fields=key,title,author_name,cover_i,first_publish_year,isbn,subject,series&limit=$maxResults"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val docs = json.optJSONArray("docs") ?: return@withContext emptyList()
            val results = mutableListOf<GoogleBookItem>()
            for (i in 0 until docs.length().coerceAtMost(maxResults)) {
                val doc = docs.getJSONObject(i)
                val title = doc.optString("title", "")
                if (title.isBlank()) continue
                val coverId = doc.optLong("cover_i", -1)
                val key = doc.optString("key", "")
                val authors = doc.optJSONArray("author_name")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }
                val firstPubYear = doc.optInt("first_publish_year", 0).takeIf { it > 0 }
                val isbns = doc.optJSONArray("isbn")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }
                val subjects = doc.optJSONArray("subject")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }

                val seriesName = doc.optJSONArray("series")?.let { arr ->
                    if (arr.length() > 0) arr.getJSONObject(0).optString("title", null) else null
                }

                var thumbnail: String? = null
                var smallThumbnail: String? = null
                if (coverId > 0) {
                    thumbnail = "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
                    smallThumbnail = "https://covers.openlibrary.org/b/id/$coverId-S.jpg"
                }

                results.add(GoogleBookItem(
                    id = key,
                    volumeInfo = VolumeInfo(
                        title = title,
                        authors = authors,
                        publishedDate = firstPubYear?.toString(),
                        description = null,
                        pageCount = doc.optInt("number_of_pages_median", 0).takeIf { it > 0 },
                        imageLinks = ImageLinks(
                            smallThumbnail = smallThumbnail,
                            thumbnail = thumbnail
                        ),
                        categories = subjects,
                        industryIdentifiers = isbns?.map { IndustryIdentifier("isbn_10", it) },
                        seriesName = seriesName
                    )
                ))
            }
            Log.d(TAG, "Open Library search: found ${results.size} results for '$query'")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Open Library", e)
            emptyList()
        }
    }

    private fun openLibraryCover(isbn: String?): String? {
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

    suspend fun fetchDescriptionBySearch(title: String, author: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("$title ${author.take(30)}", "UTF-8")
            val searchUrl = "https://openlibrary.org/search.json?q=$query&fields=key&limit=5"
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val searchRes = client.newCall(searchReq).execute()
            if (!searchRes.isSuccessful) return@withContext null
            val body = searchRes.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val docs = json.optJSONArray("docs") ?: return@withContext null
            for (i in 0 until docs.length()) {
                val key = docs.getJSONObject(i).optString("key", "")
                if (key.startsWith("/works/")) {
                    val desc = fetchDescription(key)
                    if (desc != null) return@withContext desc
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching description for '$title'", e)
            null
        }
    }

    suspend fun fetchDescriptionFromIsbn(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://openlibrary.org/isbn/$isbn.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val works = json.optJSONArray("works") ?: return@withContext null
            if (works.length() == 0) return@withContext null
            val workKey = works.getJSONObject(0).optString("key", null) ?: return@withContext null
            fetchDescription(workKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching description from ISBN $isbn", e)
            null
        }
    }

    suspend fun fetchDescription(olKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://openlibrary.org$olKey.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val desc = json.opt("description") ?: return@withContext null
            when (desc) {
                is JSONObject -> desc.optString("value", null)
                is String -> desc
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching description for $olKey", e)
            null
        }
    }

    companion object {
        private const val TAG = "PortadaResolver"
    }
}
