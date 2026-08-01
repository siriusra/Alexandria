package com.alexandria.app.data.remote

object IsbnNormalizer {

    fun toIsbn13(raw: String): String? {
        val cleaned = raw.trim().uppercase().filter { it.isDigit() || it == 'X' }
        if (cleaned.length == 10) {
            return isbn10To13(cleaned)
        }
        if (cleaned.length == 13 && cleaned.all { it.isDigit() } && isValidIsbn13(cleaned)) {
            return cleaned
        }
        return null
    }

    private fun isbn10To13(isbn10: String): String? {
        if (!isbn10.substring(0, 9).all { it.isDigit() }) return null
        val last = isbn10[9]
        if (last != 'X' && !last.isDigit()) return null

        val prefix = "978" + isbn10.substring(0, 9)
        val check = checkDigit(prefix)
        return prefix + check
    }

    private fun isValidIsbn13(isbn13: String): Boolean {
        return checkDigit(isbn13.substring(0, 12)) == isbn13[12]
    }

    private fun checkDigit(first12: String): Char {
        var sum = 0
        for (i in first12.indices) {
            val digit = first12[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val check = (10 - sum % 10) % 10
        return ('0' + check)
    }
}
