package com.alexandria.app.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DuckDuckGoCoverService {

    private val cookieJar = object : CookieJar {
        private val store = mutableMapOf<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host].orEmpty()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(cookieJar)
        .build()

    suspend fun searchCovers(query: String, maxResults: Int = 20): List<GoogleBookItem> = withContext(Dispatchers.IO) {
        try {
            val vqd = getVqdToken(query)
            if (vqd == null) {
                Log.w(TAG, "Could not get VQD token for query: $query")
                return@withContext emptyList()
            }

            val results = fetchImageResults(vqd, query, maxResults)
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
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        val match = Regex("vqd=\"?([\\w-]+)\"?").find(body)
        return match?.groupValues?.get(1)
    }

    private fun fetchImageResults(vqd: String, query: String, maxResults: Int): List<GoogleBookItem> {
        val url = "https://duckduckgo.com/i.js?l=us-en&o=json&vqd=${vqd}&f=,,,,,&p=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json,text/javascript,*/*;q=0.01")
            .header("Referer", "https://duckduckgo.com/")
            .header("X-Requested-With", "XMLHttpRequest")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        Log.d(TAG, "DDG response length: ${body.length}, starts: ${body.take(100)}")

        if (body.isBlank() || !body.trimStart().startsWith("{")) {
            Log.w(TAG, "DDG returned non-JSON response")
            return emptyList()
        }

        val json = JSONObject(body)
        if (!json.has("results")) {
            Log.w(TAG, "DDG response has no 'results' key")
            return emptyList()
        }
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
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
