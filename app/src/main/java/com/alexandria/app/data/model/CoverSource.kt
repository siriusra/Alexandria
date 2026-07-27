package com.alexandria.app.data.model

import com.google.gson.Gson

enum class CoverSource(
    val key: String,
    val label: String,
    val description: String
) {
    OPEN_LIBRARY_COVERS("openlibrary_covers", "OpenLibrary Covers", "API oficial, global, HD"),
    INTERNET_ARCHIVE("internet_archive", "Internet Archive", "Libros antiguos/raros"),
    BUSCALIBRE("buscalibre", "Buscalibre", "Chile/Colombia/México/Perú/España"),
    GANDHI("gandhi", "Gandhi", "México - catálogo grande"),
    EL_SOTANO("elsotano", "El Sótano", "México"),
    LIBRERIA_NACIONAL("librerianacional", "Librería Nacional", "Colombia"),
    OPEN_LIBRARY_SEARCH("openlibrary_search", "OpenLibrary Search", "Búsqueda por título/autor")
}

data class CoverSourceConfig(
    val enabledSources: List<CoverSource> = CoverSource.values().toList(),
    val cacheEnabled: Boolean = true,
    val cacheTtlDays: Int = 30
) {
    fun isEnabled(source: CoverSource): Boolean = enabledSources.contains(source)
    
    fun moveSource(fromIndex: Int, toIndex: Int): CoverSourceConfig {
        val newList = enabledSources.toMutableList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)
        return copy(enabledSources = newList)
    }
    
    fun toggleSource(source: CoverSource): CoverSourceConfig {
        val newList = if (enabledSources.contains(source)) {
            enabledSources.filter { it != source }
        } else {
            enabledSources + source
        }
        return copy(enabledSources = newList)
    }
    
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): CoverSourceConfig = Gson().fromJson(json, CoverSourceConfig::class.java)
    }
}