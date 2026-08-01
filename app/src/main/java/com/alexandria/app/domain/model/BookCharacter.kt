package com.alexandria.app.domain.model

data class BookCharacter(
    val id: Long = 0,
    val bookId: Long,
    val name: String,
    val iconType: String = "emoji",
    val iconKey: String = "",
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0
)
