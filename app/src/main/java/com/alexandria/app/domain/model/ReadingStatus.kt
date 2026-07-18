package com.alexandria.app.domain.model

enum class ReadingStatus(val displayName: String) {
    READING("Leyendo"),
    FINISHED("Terminado"),
    PENDING("Pendiente");

    companion object {
        fun fromString(value: String): ReadingStatus {
            return when (value.uppercase()) {
                "READING" -> READING
                "FINISHED" -> FINISHED
                "PENDING" -> PENDING
                else -> PENDING
            }
        }
    }
}
