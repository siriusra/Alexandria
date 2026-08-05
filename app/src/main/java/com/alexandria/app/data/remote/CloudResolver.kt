package com.alexandria.app.data.remote

import android.util.Log
import com.alexandria.app.domain.model.Book
import com.google.firebase.functions.FirebaseFunctions
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CloudBookMetadata(
    val coverUrl: String? = null,
    val description: String? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null,
    val ratingSource: String? = null,
    val characters: List<CloudCharacter> = emptyList()
)

data class CloudCharacter(
    val name: String,
    val isFavorite: Boolean = false,
    val emoji: String? = null
)

@Singleton
class CloudResolver @Inject constructor(
    private val functions: FirebaseFunctions
) {
    private val timeoutMs = 15_000L

    suspend fun resolveBook(uid: String, book: Book): CloudBookMetadata? = withContext(Dispatchers.IO) {
        try {
            val payload = hashMapOf<String, Any?>(
                "isbn" to book.isbn?.replace(Regex("[\\s-]"), ""),
                "titulo" to book.title,
                "autor" to book.author,
                "uid" to uid
            )
            val ref: com.google.firebase.functions.HttpsCallableReference =
                functions.getHttpsCallable("resolveBook")
            val result = ref.call(payload).await()
            parseResult(result.data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud resolve failed for '${book.title}': ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResult(data: Any?): CloudBookMetadata? {
        if (data !is Map<*, *>) return null
        val cover = data["coverUrl"] as? String
        val desc = data["description"] as? String

        val characters = (data["characters"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val name = m["name"] as? String ?: return@mapNotNull null
            CloudCharacter(
                name = name,
                isFavorite = m["isFavorite"] as? Boolean ?: false,
                emoji = m["emoji"] as? String
            )
        } ?: emptyList()

        if (cover.isNullOrBlank() && desc.isNullOrBlank() && characters.isEmpty()) return null

        return CloudBookMetadata(
            coverUrl = cover,
            description = desc,
            averageRating = (data["averageRating"] as? Number)?.toDouble(),
            ratingsCount = (data["ratingsCount"] as? Number)?.toInt(),
            ratingSource = data["ratingSource"] as? String,
            characters = characters
        )
    }

    companion object {
        private const val TAG = "CloudResolver"
    }
}