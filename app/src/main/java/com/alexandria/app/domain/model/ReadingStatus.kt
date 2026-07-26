package com.alexandria.app.domain.model

enum class ReadingStatus(val displayName: String) {
    QUIERO_LEER("Quiero leer"),
    LEYENDO("Leyendo"),
    PAUSADO("Pausado"),
    RELEYENDO("Releyendo"),
    ABANDONADO("Abandonado"),
    TERMINADO("Terminado"),
    FAVORITOS("Favoritos");

    companion object {
        fun fromString(value: String): ReadingStatus {
            return when (value.uppercase()) {
                "QUIERO_LEER", "PENDING" -> QUIERO_LEER
                "LEYENDO", "READING" -> LEYENDO
                "PAUSADO" -> PAUSADO
                "RELEYENDO" -> RELEYENDO
                "ABANDONADO" -> ABANDONADO
                "TERMINADO", "FINISHED" -> TERMINADO
                "FAVORITOS" -> FAVORITOS
                else -> QUIERO_LEER
            }
        }
    }
}
