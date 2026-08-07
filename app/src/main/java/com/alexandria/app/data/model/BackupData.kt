package com.alexandria.app.data.model

import com.alexandria.app.domain.model.Book
import com.alexandria.app.domain.model.BookCharacter

/**
 * Formato de backup completo: libros, personajes y portadas embebidas en base64.
 * El campo [coverBase64] se mapea por bookId original; al restaurar se
 * recalculan los IDs nuevos.
 */
data class BackupData(
    val version: Int = 1,
    val books: List<Book> = emptyList(),
    val characters: List<BackupCharacter> = emptyList(),
    val covers: Map<Long, String> = emptyMap()
)

data class BackupCharacter(
    val bookId: Long,
    val name: String,
    val iconType: String,
    val iconKey: String,
    val isFavorite: Boolean,
    val sortOrder: Int
) {
    fun toDomain(): BookCharacter = BookCharacter(
        bookId = bookId,
        name = name,
        iconType = iconType,
        iconKey = iconKey,
        isFavorite = isFavorite,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(c: BookCharacter): BackupCharacter = BackupCharacter(
            bookId = c.bookId,
            name = c.name,
            iconType = c.iconType,
            iconKey = c.iconKey,
            isFavorite = c.isFavorite,
            sortOrder = c.sortOrder
        )
    }
}
