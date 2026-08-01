package com.alexandria.app.data.remote

import android.util.Log
import com.alexandria.app.BuildConfig
import com.alexandria.app.data.local.CoverCacheDao
import com.alexandria.app.data.local.entity.CoverCacheEntity
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.data.model.CoverSource
import com.alexandria.app.data.model.CoverSourceConfig
import com.alexandria.app.data.remote.GoogleBooksApi
import com.alexandria.app.data.remote.GoogleBookItem
import com.alexandria.app.data.remote.VolumeInfo
import com.alexandria.app.data.remote.ImageLinks
import com.alexandria.app.data.remote.IndustryIdentifier
import com.alexandria.app.data.remote.GoogleBooksData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class PortadaResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val webClient = client.newBuilder()
        .followRedirects(true)
        .build()

    // ===== EXISTING METHODS (unchanged) =====

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

    suspend fun buscarCoversGoogleBooks(query: String, maxResults: Int = 20): List<GoogleBookItem> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
            val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
            val url = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$maxResults&printType=books$keyParam"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@withContext emptyList()

            val results = mutableListOf<GoogleBookItem>()
            for (i in 0 until items.length().coerceAtMost(maxResults)) {
                val item = items.getJSONObject(i)
                val id = item.optString("id", "")
                val vi = item.optJSONObject("volumeInfo") ?: continue
                val title = vi.optString("title", "")
                if (title.isBlank()) continue

                val authors = vi.optJSONArray("authors")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }

                val imageLinks = vi.optJSONObject("imageLinks")
                var thumbnail: String? = null
                var smallThumbnail: String? = null
                if (imageLinks != null) {
                    thumbnail = imageLinks.optString("thumbnail", null)
                    smallThumbnail = imageLinks.optString("smallThumbnail", null)
                }

                val identifiers = vi.optJSONArray("industryIdentifiers")?.let { arr ->
                    (0 until arr.length()).mapNotNull {
                        val obj = arr.optJSONObject(it) ?: return@mapNotNull null
                        val type = obj.optString("type", "")
                        val identifier = obj.optString("identifier", "")
                        if (type.isNotBlank() && identifier.isNotBlank())
                            IndustryIdentifier(type, identifier)
                        else null
                    }
                }

                val publishedDate = vi.optString("publishedDate", null)
                val pageCount = vi.optInt("pageCount", 0).takeIf { it > 0 }
                val categories = vi.optJSONArray("categories")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                }

                results.add(GoogleBookItem(
                    id = id,
                    volumeInfo = VolumeInfo(
                        title = title,
                        authors = authors,
                        publishedDate = publishedDate,
                        description = null,
                        pageCount = pageCount,
                        imageLinks = ImageLinks(
                            smallThumbnail = smallThumbnail,
                            thumbnail = thumbnail
                        ),
                        categories = categories,
                        industryIdentifiers = identifiers,
                        seriesName = null
                    )
                ))
            }
            Log.d(TAG, "Google Books search: found ${results.size} results for '$query'")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Google Books covers", e)
            emptyList()
        }
    }

    // ===== NEW COVER RESOLVER WITH 7 SOURCES =====

    suspend fun resolverCover(
        isbn: String?,
        titulo: String,
        autor: String?,
        coverCacheDao: CoverCacheDao?,
        config: CoverSourceConfig = CoverSourceConfig()
    ): String? = withContext(Dispatchers.IO) {
        val cleanIsbn = isbn?.replace(Regex("[\\s-]"), "")

        if (cleanIsbn.isNullOrBlank()) {
            return@withContext resolverCoverBySearch(titulo, autor, config)
        }

        // Check cache first
        if (config.cacheEnabled) {
            coverCacheDao?.let { dao ->
                val cached = dao.get(cleanIsbn)
                if (cached != null && !isCacheExpired(cached, config.cacheTtlDays)) {
                    Log.d(TAG, "Cover cache hit for ISBN $cleanIsbn from ${cached.source}")
                    return@withContext cached.coverUrl
                }
            }
        }

        // Priority chain based on enabled sources order
        for (source in config.enabledSources) {
            val url = when (source) {
                CoverSource.OPEN_LIBRARY_COVERS -> fetchOpenLibraryCover(cleanIsbn)
                CoverSource.INTERNET_ARCHIVE -> fetchInternetArchiveCover(cleanIsbn)
                CoverSource.BUSCALIBRE -> fetchBuscalibreCover(cleanIsbn)
                CoverSource.GANDHI -> fetchGandhiCover(cleanIsbn)
                CoverSource.EL_SOTANO -> fetchElSotanoCover(cleanIsbn)
                CoverSource.LIBRERIA_NACIONAL -> fetchLibreriaNacionalCover(cleanIsbn)
                CoverSource.OPEN_LIBRARY_SEARCH -> fetchOpenLibrarySearchCover(titulo, autor)
            }

            if (url != null && url.isNotBlank()) {
                // Cache the result
                if (config.cacheEnabled) {
                    coverCacheDao?.put(CoverCacheEntity(cleanIsbn, url, source.key))
                }
                Log.d(TAG, "Cover found for ISBN $cleanIsbn from ${source.label}")
                return@withContext url
            }
        }

        // Fallback to search if ISBN-based sources failed
        resolverCoverBySearch(titulo, autor, config)
    }

    private suspend fun resolverCoverBySearch(
        titulo: String,
        autor: String?,
        config: CoverSourceConfig
    ): String? = withContext(Dispatchers.IO) {
        if (config.enabledSources.contains(CoverSource.OPEN_LIBRARY_SEARCH)) {
            return@withContext fetchOpenLibrarySearchCover(titulo, autor)
        }
        null
    }

    private fun isCacheExpired(entity: CoverCacheEntity, ttlDays: Int): Boolean {
        val cutoff = System.currentTimeMillis() - (ttlDays.toLong() * 24 * 60 * 60 * 1000)
        return entity.timestamp < cutoff
    }

    // ===== 7 COVER SOURCE IMPLEMENTATIONS =====

    private suspend fun fetchOpenLibraryCover(isbn: String): String? = withContext(Dispatchers.IO) {
        openLibraryCover(isbn)
    }

    private suspend fun fetchInternetArchiveCover(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedIsbn = java.net.URLEncoder.encode(isbn, "UTF-8")
            val searchUrl = "https://archive.org/advancedsearch.php?q=isbn:$encodedIsbn&output=json&rows=1"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val docs = json.optJSONObject("response")?.optJSONArray("docs") ?: return@withContext null
            if (docs.length() == 0) return@withContext null
            val identifier = docs.getJSONObject(0).optString("identifier", "") ?: return@withContext null
            return@withContext "https://archive.org/services/img/$identifier"
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Internet Archive cover for ISBN $isbn", e)
            null
        }
    }

    private suspend fun fetchBuscalibreCover(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://www.buscalibre.com.mx/busquedas?isbn=$isbn"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = webClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            val productLink = doc.selectFirst("a[href*=/libro-], a[href*=/p/]")?.attr("abs:href")
                ?: return@withContext null

            val prodRequest = Request.Builder()
                .url(productLink)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val prodResponse = webClient.newCall(prodRequest).execute()
            if (!prodResponse.isSuccessful) return@withContext null
            val prodHtml = prodResponse.body?.string() ?: return@withContext null
            val prodDoc = Jsoup.parse(prodHtml)

            return@withContext extractCoverFromDoc(prodDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Buscalibre cover for ISBN $isbn", e)
            null
        }
    }

    private suspend fun fetchGandhiCover(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://www.gandhi.com.mx/busqueda?q=$isbn"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = webClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            val productLink = doc.selectFirst("a[href*=/producto/], a[href*=/libro/]")?.attr("abs:href")
                ?: return@withContext null

            val prodRequest = Request.Builder()
                .url(productLink)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val prodResponse = webClient.newCall(prodRequest).execute()
            if (!prodResponse.isSuccessful) return@withContext null
            val prodHtml = prodResponse.body?.string() ?: return@withContext null
            val prodDoc = Jsoup.parse(prodHtml)

            return@withContext extractCoverFromDoc(prodDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Gandhi cover for ISBN $isbn", e)
            null
        }
    }

    private suspend fun fetchElSotanoCover(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://www.elsotano.com/busqueda?q=$isbn"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = webClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            val productLink = doc.selectFirst("a[href*=/libro-], a[href*=/p/]")?.attr("abs:href")
                ?: return@withContext null

            val prodRequest = Request.Builder()
                .url(productLink)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val prodResponse = webClient.newCall(prodRequest).execute()
            if (!prodResponse.isSuccessful) return@withContext null
            val prodHtml = prodResponse.body?.string() ?: return@withContext null
            val prodDoc = Jsoup.parse(prodHtml)

            return@withContext extractCoverFromDoc(prodDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching El Sótano cover for ISBN $isbn", e)
            null
        }
    }

    private suspend fun fetchLibreriaNacionalCover(isbn: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://www.librerianacional.com/buscar?q=$isbn"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = webClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html)

            val productLink = doc.selectFirst("a[href*=/producto/], a.product-link")?.attr("abs:href")
                ?: return@withContext null

            val prodRequest = Request.Builder()
                .url(productLink)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val prodResponse = webClient.newCall(prodRequest).execute()
            if (!prodResponse.isSuccessful) return@withContext null
            val prodHtml = prodResponse.body?.string() ?: return@withContext null
            val prodDoc = Jsoup.parse(prodHtml)

            return@withContext extractCoverFromDoc(prodDoc)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Librería Nacional cover for ISBN $isbn", e)
            null
        }
    }

    private suspend fun fetchOpenLibrarySearchCover(title: String, author: String?): String? = withContext(Dispatchers.IO) {
        try {
            val authorPart = author?.takeIf { it.isNotBlank() }?.take(30) ?: ""
            val query = java.net.URLEncoder.encode("$title $authorPart", "UTF-8")
            val url = "https://openlibrary.org/search.json?q=$query&fields=cover_i&limit=1&language=spa"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val docs = json.optJSONArray("docs") ?: return@withContext null
            if (docs.length() == 0) return@withContext null
            val coverId = docs.getJSONObject(0).optLong("cover_i", -1)
            if (coverId > 0) {
                return@withContext "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OpenLibrary search cover for '$title'", e)
            null
        }
    }

    private fun extractCoverFromDoc(doc: org.jsoup.nodes.Document): String? {
        val selectors = listOf(
            "meta[property=og:image]",
            "meta[name=twitter:image]",
            "[itemprop=image]",
            ".product-image img",
            ".book-cover img",
            ".cover img",
            "img.portada",
            "img.cover",
            ".image-container img",
            "img[src*='cover']",
            "img[src*='portada']"
        )

        for (selector in selectors) {
            val el = doc.selectFirst(selector)
            if (el != null) {
                val url = if (el.tagName() == "meta") el.attr("content") else el.attr("abs:src")
                if (url.isNotBlank() && !url.contains("placeholder") && !url.contains("no-image")) {
                    return url
                }
            }
        }
        return null
    }

    // ===== EXISTING DESCRIPTION METHODS (unchanged) =====

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

    // ===== SPANISH LANGUAGE DETECTION =====

    fun isSpanishText(text: String): Boolean {
        val lower = text.lowercase()
        val hasDiacritics = Regex("[áéíóúñüÁÉÍÓÚÑÜ]").containsMatchIn(text)
        if (hasDiacritics && lower.length > 40) return true

        val spanishWords = listOf(
            "el", "la", "los", "las", "de", "del", "que", "con", "por", "para",
            "una", "un", "es", "su", "se", "como", "cuando", "entre", "más",
            "pero", "sin", "sobre", "también", "después", "tras", "mientras",
            "ella", "nos", "les", "al", "no", "ya", "todo", "esta", "este",
            "historia", "novela", "mundo", "vida", "hombre", "mujer", "padre",
            "madre", "hermano", "amor", "tiempo", "años", "días", "noche", "día"
        )
        val englishWords = listOf(
            "the", "and", "of", "to", "in", "is", "are", "was", "were", "with",
            "from", "for", "this", "that", "it", "his", "her", "they", "you",
            "we", "story", "world", "life", "when", "about", "after", "into",
            "their", "there", "them", "had", "has", "have", "been", "will",
            "would", "could", "should", "father", "mother", "brother", "love",
            "time", "years", "days", "night", "man", "woman"
        )
        var spanishScore = 0
        var englishScore = 0
        for (word in spanishWords) {
            spanishScore += Regex("\\b$word\\b").findAll(lower).count()
        }
        for (word in englishWords) {
            englishScore += Regex("\\b$word\\b").findAll(lower).count()
        }
        return spanishScore >= englishScore && spanishScore > 0
    }

    private fun String.normalizeForMatch(): String =
        lowercase().trim().replace(Regex("[^a-záéíóúñü0-9\\s]"), "")

    private fun shareSignificantWord(a: String, b: String): Boolean {
        val wordsA = a.split(" ").filter { it.length > 3 }.toSet()
        val wordsB = b.split(" ").filter { it.length > 3 }.toSet()
        return wordsA.any { it in wordsB }
    }

    private fun JSONArray.findBestPage(title: String, requireTitleOverlap: Boolean = false): String? {
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
            if (bookKey == null && isBook && (!requireTitleOverlap || shareSignificantWord(normPageTitle, normTitle))) {
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
            val normalizedIsbn = IsbnNormalizer.toIsbn13(isbn) ?: return@withContext null
            val url = "https://openlibrary.org/isbn/$normalizedIsbn.json"
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
            val cleanIsbn = IsbnNormalizer.toIsbn13(isbn) ?: return@withContext null
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

    suspend fun fetchFromGoogleBooks(title: String, author: String, isbn: String? = null): GoogleBooksData? = withContext(Dispatchers.IO) {
        try {
            val normalizedIsbn = isbn?.let { IsbnNormalizer.toIsbn13(it) }
            val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
            val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""

            val queries = buildList {
                normalizedIsbn?.let { add("isbn:$it") }
                add("$title $author")
            }

            for (query in queries) {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://www.googleapis.com/books/v1/volumes?q=$encoded&langRestrict=es&maxResults=5&printType=books$keyParam"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) continue
                val body = response.body?.string() ?: continue
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: continue

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
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Google Books data for '$title'", e)
            null
        }
    }

    // ===== CHARACTER EXTRACTION =====

    suspend fun fetchCharacters(title: String, author: String): List<String> = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000) {
                if (title.isBlank()) return@withTimeout emptyList()

                val mainKey = findBookPageKey(title, author)
                if (mainKey != null) {
                    val wikidata = fetchWikidataCharacters(mainKey)
                    if (wikidata.isNotEmpty()) {
                        Log.d(TAG, "Characters (Wikidata) for '$title': ${wikidata.take(12)}")
                        return@withTimeout wikidata.take(12)
                    }
                }

                val candidates = mutableListOf<String>()

                if (mainKey != null) {
                    candidates.addAll(parsePersonajesSection(mainKey, allContent = false))
                }

                val anexoKey = findAnexoPageKey(title)
                if (anexoKey != null) {
                    candidates.addAll(parsePersonajesSection(anexoKey, allContent = true))
                }

                val cleaned = candidates
                    .map { cleanCharacterName(it) }
                    .filter { it.length in 2..60 }
                    .filter { it.split(" ").size <= 6 }
                    .filterNot { isStructuralJunk(it) }
                    .distinct()

                Log.d(TAG, "Characters (Wikipedia) for '$title': ${cleaned.take(12)}")
                cleaned.take(12)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching characters for '$title'", e)
            emptyList()
        }
    }

    private suspend fun fetchWikidataItemId(pageKey: String): String? {
        return try {
            val encodedKey = java.net.URLEncoder.encode(pageKey, "UTF-8").replace("+", "%20")
            val url = "https://es.wikipedia.org/api/rest_v1/page/summary/$encodedKey"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string() ?: return null)
            val qid = json.optString("wikibase_item", null)
            qid?.takeIf { it.startsWith("Q") && it.length > 1 }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Wikidata item id for $pageKey", e)
            null
        }
    }

    private suspend fun fetchWikidataCharacters(pageKey: String): List<String> {
        val qid = fetchWikidataItemId(pageKey) ?: return emptyList()
        return try {
            val claimsUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=$qid&props=claims&format=json"
            val claimsReq = Request.Builder()
                .url(claimsUrl)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val claimsRes = client.newCall(claimsReq).execute()
            if (!claimsRes.isSuccessful) return emptyList()
            val claimsJson = JSONObject(claimsRes.body?.string() ?: return emptyList())
            val entity = claimsJson.optJSONObject("entities")?.optJSONObject(qid) ?: return emptyList()
            val p31 = entity.optJSONObject("claims")?.optJSONArray("P31")
            if (p31 != null) {
                for (i in 0 until p31.length()) {
                    val claim = p31.optJSONObject(i) ?: continue
                    val value = claim
                        .optJSONObject("mainsnak")
                        ?.optJSONObject("datavalue")
                        ?.optJSONObject("value")
                    val id = value?.optString("id", null)
                    if (id != null && id in NON_BOOK_P31) {
                        Log.d(TAG, "Skipping Wikidata item $qid: type $id is not a book")
                        return emptyList()
                    }
                }
            }
            val p674 = entity.optJSONObject("claims")?.optJSONArray("P674") ?: return emptyList()

            val charIds = LinkedHashSet<String>()
            for (i in 0 until p674.length()) {
                val claim = p674.optJSONObject(i) ?: continue
                val value = claim
                    .optJSONObject("mainsnak")
                    ?.optJSONObject("datavalue")
                    ?.optJSONObject("value")
                val id = value?.optString("id", null)
                if (id != null && id.startsWith("Q")) charIds.add(id)
            }
            if (charIds.isEmpty()) return emptyList()

            val labelsUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${charIds.joinToString("|")}&props=labels&languages=es%7Cen&format=json"
            val labelsReq = Request.Builder()
                .url(labelsUrl)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val labelsRes = client.newCall(labelsReq).execute()
            if (!labelsRes.isSuccessful) return emptyList()
            val labelsJson = JSONObject(labelsRes.body?.string() ?: return emptyList())
            val entities = labelsJson.optJSONObject("entities") ?: return emptyList()

            val names = LinkedHashSet<String>()
            for (id in charIds) {
                val labels = entities.optJSONObject(id)?.optJSONObject("labels") ?: continue
                val label = labels.optJSONObject("es") ?: labels.optJSONObject("en") ?: continue
                val raw = label.optString("value", null) ?: continue
                val name = cleanCharacterName(raw)
                if (name.length !in 2..60) continue
                if (isStructuralJunk(name)) continue
                if (raw.contains("personajes de", ignoreCase = true) ||
                    raw.contains("list of", ignoreCase = true) ||
                    raw.contains("characters of", ignoreCase = true) ||
                    raw.contains("anexo", ignoreCase = true)
                ) continue
                names.add(name)
            }
            names.toList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Wikidata characters for $qid", e)
            emptyList()
        }
    }

    private suspend fun findBookPageKey(title: String, author: String): String? {
        val queries = buildList {
            add("\"$title\" $author")
            add("\"$title\" libro")
            add("\"$title\"")
        }
        for (query in queries) {
            val pages = searchWikipediaPage(query) ?: continue
            if (pages.length() == 0) continue
            val bestKey = pages.findBestPage(title, requireTitleOverlap = true)
            if (bestKey != null) return bestKey
        }
        return null
    }

    private suspend fun findAnexoPageKey(title: String): String? {
        val normTitle = title.normalizeForMatch()
        val queries = listOf(
            "Anexo:Personajes de $title",
            "Anexo:Personajes de \"$title\"",
            "$title personajes anexo"
        )
        for (query in queries) {
            val pages = searchWikipediaPage(query) ?: continue
            if (pages.length() == 0) continue
            for (i in 0 until pages.length()) {
                val page = pages.getJSONObject(i)
                val pageTitle = page.optString("title", "")
                val key = page.optString("key", null)
                if (key == null) continue
                val normAnnexo = pageTitle.normalizeForMatch()
                if (normAnnexo.contains("personajes") && shareSignificantWord(normAnnexo, normTitle)) {
                    return key
                }
            }
        }
        return null
    }

    private fun parsePersonajesSection(pageKey: String, allContent: Boolean): List<String> {
        try {
            val encodedKey = java.net.URLEncoder.encode(pageKey, "UTF-8").replace("+", "%20")
            val url = "https://es.wikipedia.org/wiki/$encodedKey"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = webClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)
            val names = LinkedHashSet<String>()

            val content = doc.selectFirst("div.mw-parser-output")
            if (content == null) return emptyList()

            if (allContent) {
                for (li in content.select("ul > li")) {
                    addNameFromLi(li, names, maxLen = 40)
                }
                for (td in content.select("table.wikitable tr > td:first-child")) {
                    addNameFromTd(td, names)
                }
            } else {
                val heading = content.select("h2, h3, h4").firstOrNull {
                    it.text().contains("Personajes", ignoreCase = true)
                }
                if (heading != null) {
                    val level = heading.tagName().substring(1).toIntOrNull() ?: 2
                    var el = heading.nextElementSibling()
                    while (el != null) {
                        val tag = el.tagName()
                        if (tag.startsWith("h")) {
                            val lvl = tag.substring(1).toIntOrNull() ?: level
                            if (lvl <= level) break
                        }
                        if (tag == "ul") {
                            for (li in el.select("> li")) {
                                addNameFromLi(li, names)
                            }
                        }
                        if (tag == "table" && el.classNames().contains("wikitable")) {
                            for (td in el.select("td")) {
                                addNameFromTd(td, names)
                            }
                        }
                        el = el.nextElementSibling()
                    }
                }
            }

            return names.toList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing characters page $pageKey", e)
            return emptyList()
        }
    }

    private fun addNameFromLi(
        li: org.jsoup.nodes.Element,
        names: LinkedHashSet<String>,
        maxLen: Int = 120
    ) {
        val bold = li.selectFirst("b")
        val text = if (bold != null) bold.text() else li.text()
        val clean = text.trim()
        if (clean.isNotBlank() && clean.length <= maxLen) names.add(clean)
    }

    private fun addNameFromTd(td: org.jsoup.nodes.Element, names: LinkedHashSet<String>) {
        val text = td.text().trim()
        if (text.isNotBlank() && text.length in 2..60 && !text.contains(":")) {
            names.add(text)
        }
    }

    private fun cleanCharacterName(raw: String): String {
        var name = raw.trim()
        for (sep in listOf(" — ", " – ", " - ", ": ", ":", ";", ",", "(", "[", "«", "\"")) {
            val idx = name.indexOf(sep)
            if (idx > 0) {
                name = name.substring(0, idx)
                break
            }
        }
        return name.trim().trimEnd('.', ';')
    }

    private fun isStructuralJunk(name: String): Boolean {
        val lower = name.lowercase()
        val junkWords = listOf(
            "véase", "referencias", "enlaces externos", "bibliografía", "notas",
            "anexo", "personajes principales", "personajes secundarios", "categoría",
            "galardón", "premios", "recepción", "análisis", "adaptación", "resumen",
            "sinopsis", "ambientación", "inspiración", "estilo", "argumento",
            "trama", "ediciones", "traducciones", "lista de", "plantilla",
            "categorías", "enlaces", "wikipedia", "fuentes", "véase también"
        )
        return junkWords.any { lower.startsWith(it) || lower.contains(" $it") }
    }

    companion object {
        private const val TAG = "PortadaResolver"

        private val NON_BOOK_P31 = setOf(
            "Q11424",     // película
            "Q7889",      // videojuego
            "Q5398426",   // serie de televisión
            "Q1107",      // anime
            "Q8274",      // manga
            "Q95074",     // personaje de ficción
            "Q482994",    // álbum
            "Q7366"       // canción
        )
    }
}