package com.alexandria.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DuckDuckGoCoverService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun searchCovers(query: String, maxResults: Int = 20): List<GoogleBookItem> = withContext(Dispatchers.IO) {
        try {
            val vqd = getVqdToken(query)
            if (vqd == null) {
                Log.w(TAG, "Could not get VQD token for query: $query")
                return@withContext emptyList()
            }

            val results = fetchImageResults(vqd, maxResults)
            Log.d(TAG, "DuckDuckGo found ${results.size} covers for '$query'")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching DuckDuckGo for: $query", e)
            emptyList()
        }
    }

    private fun getVqdToken(query: String): String? {
        val url = "https://duckduckgo.com/?q=${query.replace(" ", "+")}&iax=images&ia=images"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        val match = Regex("vqd=([\\w-]+)").find(body)
        return match?.groupValues?.get(1)
    }

    private fun fetchImageResults(vqd: String, maxResults: Int): List<GoogleBookItem> {
        val url = "https://duckduckgo.com/i.js?l=us-en&o=json&vqd=${vqd}&f=,,,,,&p=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val json = JSONObject(body)
        val results = json.getJSONArray("results")

        val items = mutableListOf<GoogleBookItem>()
        for (i in 0 until minOf(results.length(), maxResults)) {
            val result = results.getJSONObject(i)
            val title = result.optString("title", "")
            val imageUrl = result.optString("image", "")
            val thumbnailUrl = result.optString("thumbnail", "")

            if (imageUrl.isNotBlank() && title.isNotBlank()) {
                items.add(
                    GoogleBookItem(
                        id = "ddg_$i",
                        volumeInfo = VolumeInfo(
                            title = title,
                            authors = listOf("Web Result"),
                            publishedDate = null,
                            description = null,
                            pageCount = null,
                            imageLinks = ImageLinks(
                                smallThumbnail = thumbnailUrl,
                                thumbnail = imageUrl
                            ),
                            categories = null,
                            industryIdentifiers = null
                        )
                    )
                )
            }
        }

        return items
    }

    companion object {
        private const val TAG = "DuckDuckGoCoverService"
        private const val USER_AGENT = "Alexandria/1.0 (Android Book Tracker)"
    }
}
