package com.alexandria.app.ui.theme

import androidx.compose.ui.graphics.Color

object GenreColorMapper {

    private val genrePalette = listOf(
        Color(0xFFC62828),
        Color(0xFFAD1457),
        Color(0xFF6A1B9A),
        Color(0xFF4527A0),
        Color(0xFF283593),
        Color(0xFF1565C0),
        Color(0xFF00838F),
        Color(0xFF00695C),
        Color(0xFF2E7D32),
        Color(0xFF558B2F),
        Color(0xFF9E9D24),
        Color(0xFFF9A825),
        Color(0xFFEF6C00),
        Color(0xFFD84315),
        Color(0xFF4E342E),
        Color(0xFF37474F)
    )

    private val genreColorCache = mutableMapOf<String, Color>()

    fun colorFor(genre: String?): Color {
        if (genre.isNullOrBlank()) return genrePalette.first()
        return genreColorCache.getOrPut(genre.lowercase().trim()) {
            val hash = genre.lowercase().trim().hashCode()
            val index = (hash and Int.MAX_VALUE) % genrePalette.size
            genrePalette[index]
        }
    }

    val shelfColor: Color = Color(0xFF8D6E63)

    val shelfColorDark: Color = Color(0xFF5D4037)

    val woodGrain: Color = Color(0xFF6D4C41)
}
