package com.alexandria.app.data.remote

import android.util.Log
import com.alexandria.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class GoogleBooksData(
    val description: String?,
    val averageRating: Double?,
    val ratingsCount: Int?
)

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

    suspend fun fetchDescriptionBySearch(title: String, author: String, lang: String? = null): GoogleBooksData? = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("$title ${author.take(30)}", "UTF-8")
            var searchUrl = "https://openlibrary.org/search.json?q=$query&fields=key,ratings_average,ratings_count,description&limit=10"
            if (lang != null) searchUrl += "&language=$lang"
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
                val doc = docs.getJSONObject(i)
                val key = doc.optString("key", "")
                var rating: Double? = null
                var ratingCount: Int? = null
                val rawRating = doc.opt("ratings_average")
                if (rawRating is Number) rating = rawRating.toDouble()
                val rawCount = doc.opt("ratings_count")
                if (rawCount is Number) ratingCount = rawCount.toInt()
                if (key.startsWith("/works/")) {
                    val desc = fetchDescription(key)
                    return@withContext GoogleBooksData(
                        description = desc,
                        averageRating = rating,
                        ratingsCount = ratingCount
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching description for '$title'", e)
            null
        }
    }

    private fun String.normalizeForMatch(): String =
        lowercase().trim().replace(Regex("[^a-záéíóúñü0-9\\s]"), "")

    private fun JSONArray.findBestPage(title: String): String? {
        val normTitle = title.normalizeForMatch()
        val bookKeywords = listOf(
            "novela", "libro", "cuento", "obra literaria", "literatura", "relato",
            "obra", "ensayo", "biografía", "tratado", "manual", "guía",
            "compendio", "saga", "serie literaria", "poemario", "drama",
            "comedia", "tragedia", "fábula", "leyenda", "mito",
            "novela gráfica", "historieta", "divulgación"
        )
        val personKeywords = listOf(
            "escritor", "periodista", "poeta", "actor", "música", "pintor",
            "futbolista", "director", "cantante", "músico"
        )
        val rejectKeywords = listOf(
            "videojuego", "álbum", "canción", "disco", "película", "serie de televisión",
            "anime", "manga", "programa de televisión", "premio", "personaje",
            "telenovela", "cortometraje", "documental", "concierto",
            "banda", "grupo musical", "sencillo", "gira",
            "deporte", "equipo", "jugador"
        )

        var exactKey: String? = null
        var containsKey: String? = null
        var bookKey: String? = null

        for (i in 0 until length()) {
            val page = getJSONObject(i)
            val pageTitle = page.optString("title", "").lowercase().trim()
            val normPageTitle = pageTitle.normalizeForMatch()
            val pageDesc = page.optString("description", null)
            val desc = pageDesc?.lowercase() ?: ""
            val hasDesc = pageDesc != null

            val isPerson = hasDesc && personKeywords.any { desc.contains(it) }
            val isReject = hasDesc && rejectKeywords.any { desc.contains(it) }
            if (isPerson || isReject) continue

            val isBook = hasDesc && bookKeywords.any { desc.contains(it) }

            if (normPageTitle == normTitle) {
                exactKey = page.optString("key", null)
                break
            }
            if (containsKey == null && normPageTitle.contains(normTitle)) {
                containsKey = page.optString("key", null)
            }
            if (bookKey == null && isBook) {
                bookKey = page.optString("key", null)
            }
        }

        return exactKey ?: containsKey ?: bookKey
    }

    private suspend fun searchWikipediaPage(query: String): JSONArray? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.wikimedia.org/core/v1/wikipedia/es/search/page?q=$encodedQuery&limit=5"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        return json.optJSONArray("pages")
    }

    private suspend fun fetchWikipediaExtract(key: String): String? {
        val encodedKey = java.net.URLEncoder.encode(key, "UTF-8").replace("+", "%20")
        val url = "https://es.wikipedia.org/api/rest_v1/page/summary/$encodedKey"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val extract = json.optString("extract", null)
        return if (extract != null && extract.isNotBlank()) extract else null
    }

    suspend fun fetchDescriptionFromWikipedia(title: String, author: String): String? = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank()) return@withContext null

            val queries = buildList {
                add("\"$title\" $author")
                add("\"$title\" libro")
                add("\"$title\"")
                add("$title libro")
                add("$title $author")
            }

            for (query in queries) {
                val pages = searchWikipediaPage(query) ?: continue
                if (pages.length() == 0) continue
                val bestKey = pages.findBestPage(title)
                if (bestKey != null) {
                    val extract = fetchWikipediaExtract(bestKey)
                    if (extract != null) return@withContext extract
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Wikipedia description for '$title'", e)
            null
        }
    }

    suspend fun fetchDescriptionFromCasaDelLibro(title: String, author: String): String? = withContext(Dispatchers.IO) {
        try {
            if (title.isBlank()) return@withContext null
            val query = java.net.URLEncoder.encode("$title $author", "UTF-8")
            val searchUrl = "https://www.casadellibro.com/busqueda?q=$query"

            val webClient = client.newBuilder().followRedirects(true).build()
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val searchRes = webClient.newCall(searchReq).execute()
            if (!searchRes.isSuccessful) return@withContext null
            val html = searchRes.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            val lowerTitle = title.lowercase().trim()
            var bookUrl: String? = null
            for (link in doc.select("a[href]")) {
                val href = link.attr("href")
                val text = link.text().lowercase().trim()
                if (href.contains("/libro-") && (text == lowerTitle || text.contains(lowerTitle))) {
                    bookUrl = if (href.startsWith("/")) "https://www.casadellibro.com$href" else href
                    break
                }
            }
            if (bookUrl == null) {
                val first = doc.selectFirst("a[href*=/libro-]")
                if (first != null) {
                    val href = first.attr("href")
                    bookUrl = if (href.startsWith("/")) "https://www.casadellibro.com$href" else href
                }
            }
            if (bookUrl == null) return@withContext null

            val prodReq = Request.Builder()
                .url(bookUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val prodRes = webClient.newCall(prodReq).execute()
            if (!prodRes.isSuccessful) return@withContext null
            val prodHtml = prodRes.body?.string() ?: return@withContext null
            val prodDoc = Jsoup.parse(prodHtml)

            for (selector in listOf(
                "meta[property=og:description]",
                "meta[name=description]",
                "[itemprop=description]",
                ".product-description",
                ".descripcion",
                ".sinopsis"
            )) {
                val el = prodDoc.selectFirst(selector)
                if (el != null) {
                    val text = if (el.tagName() == "meta") el.attr("content") else el.text()
                    if (text.isNotBlank()) return@withContext text
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Casa del Libro description for '$title'", e)
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

    suspend fun fetchDescriptionFromTodoTusLibros(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanIsbn = isbn.replace(Regex("[\\s-]"), "")
            val url = "https://www.todostuslibros.com/busquedas?isbn=$cleanIsbn"
            val redirectClient = client.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = redirectClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val finalUrl = response.request.url.toString()
            if (finalUrl.contains("/busquedas?")) return@withContext null

            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            for (script in doc.select("script[type=application/ld+json]")) {
                try {
                    val raw = script.html().trim()
                    if (raw.startsWith("[")) {
                        val arr = JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            if (item.optString("@type") == "Book") {
                                val desc = item.optString("description", null)
                                if (!desc.isNullOrBlank()) return@withContext desc
                            }
                        }
                    } else {
                        val item = JSONObject(raw)
                        if (item.optString("@type") == "Book") {
                            val desc = item.optString("description", null)
                            if (!desc.isNullOrBlank()) return@withContext desc
                        }
                    }
                } catch (_: Exception) { }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TodoTusLibros description for ISBN $isbn", e)
            null
        }
    }

    suspend fun fetchFromGoogleBooks(title: String, author: String): GoogleBooksData? = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("$title $author", "UTF-8")
            val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
            val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
            val url = "https://www.googleapis.com/books/v1/volumes?q=$query&langRestrict=es&maxResults=5&printType=books$keyParam"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext null

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val vi = item.optJSONObject("volumeInfo") ?: continue
                val itemTitle = vi.optString("title", "").lowercase()
                val itemDesc = vi.optString("description", null)
                val itemRating = vi.optDouble("averageRating", -1.0)
                val itemRatingsCount = vi.optInt("ratingsCount", -1)

                val titleWords = title.lowercase().split(" ").filter { it.length > 2 }
                val matchScore = titleWords.count { word -> itemTitle.contains(word) }
                if (matchScore == 0 || (itemDesc == null && itemRating < 0)) continue

                return@withContext GoogleBooksData(
                    description = itemDesc,
                    averageRating = if (itemRating >= 0) itemRating else null,
                    ratingsCount = if (itemRatingsCount > 0) itemRatingsCount else null
                )
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Google Books data for '$title'", e)
            null
        }
    }

    companion object {
        private const val TAG = "PortadaResolver"
    }
}
