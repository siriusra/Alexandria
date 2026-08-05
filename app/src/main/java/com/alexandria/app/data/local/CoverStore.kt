package com.alexandria.app.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverStore @Inject constructor(
    private val context: Context
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val coversDir: File by lazy {
        File(context.filesDir, "covers").apply { mkdirs() }
    }

    /** Nombre de archivo estable por ISBN; si no hay ISBN, deriva de un hash de la URL. */
    private fun fileName(isbn: String?, url: String): String {
        val key = isbn?.replace(Regex("[\\s-]"), "")?.takeIf { it.isNotBlank() }
        val base = key?.takeIf { it.length >= 4 } ?: sha256(url)
        val safe = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "$safe.jpg"
    }

    fun localPath(isbn: String?, url: String): String? {
        val name = fileName(isbn, url)
        val file = File(coversDir, name)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    /** Descarga la imagen de la portada a almacenamiento interno y devuelve la ruta absoluta. */
    suspend fun saveCover(isbn: String?, url: String): String? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        val file = File(coversDir, fileName(isbn, url))
        if (file.exists() && file.length() > 0) {
            return@withContext file.absolutePath
        }
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Alexandria/1.0 (Android Book Tracker)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val bytes = body.bytes()
            if (bytes.isEmpty()) return@withContext null
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun clearAll() {
        if (coversDir.exists()) coversDir.deleteRecursively()
    }

    private fun sha256(input: String): String = try {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        digest.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        input.hashCode().toString()
    }
}
