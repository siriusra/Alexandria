package com.alexandria.app.data.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser

enum class CoverSource(
    val key: String,
    val label: String,
    val description: String
) {
    OPEN_LIBRARY_COVERS("openlibrary_covers", "OpenLibrary Covers", "API oficial, global, HD"),
    INTERNET_ARCHIVE("internet_archive", "Internet Archive", "Libros antiguos/raros"),
    BNE("bne", "BNE (España)", "Catálogo oficial de la Biblioteca Nacional de España (SPARQL)"),
    GOOGLE_BOOKS("google_books", "Google Books", "API oficial con restricción de idioma"),
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
        private val knownByName: Map<String, CoverSource> = CoverSource.values().associateBy { it.name }

        fun fromJson(json: String): CoverSourceConfig = try {
            val root: JsonElement = JsonParser.parseString(json)
            val obj = root.asJsonObject
            val arr = obj.getAsJsonArray("enabledSources")
            val parsed = arr.mapNotNull { elem ->
                elem?.asJsonPrimitive?.asString?.let { knownByName[it] }
            }
            val enabled = if (parsed.isEmpty()) CoverSource.values().toList() else parsed
            val cacheEnabled = obj.get("cacheEnabled")?.asBoolean ?: true
            val cacheTtlDays = obj.get("cacheTtlDays")?.asInt ?: 30
            CoverSourceConfig(
                enabledSources = enabled,
                cacheEnabled = cacheEnabled,
                cacheTtlDays = cacheTtlDays
            )
        } catch (e: Exception) {
            CoverSourceConfig()
        }
    }
}
