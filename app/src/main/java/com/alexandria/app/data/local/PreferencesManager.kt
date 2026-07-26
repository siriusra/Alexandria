package com.alexandria.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SynopsisSourceConfig(
    val isbn: Boolean = true,
    val casaDelLibro: Boolean = true,
    val openLibrary: Boolean = true,
    val wikipedia: Boolean = true,
    val todostuslibros: Boolean = true,
    val googleBooks: Boolean = true
)

class PreferencesManager(private val context: Context) {

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val SYNOPSIS_ISBN = booleanPreferencesKey("synopsis_isbn")
        val SYNOPSIS_CASA_DEL_LIBRO = booleanPreferencesKey("synopsis_casa_del_libro")
        val SYNOPSIS_OPENLIBRARY = booleanPreferencesKey("synopsis_openlibrary")
        val SYNOPSIS_WIKIPEDIA = booleanPreferencesKey("synopsis_wikipedia")
        val SYNOPSIS_TODOSTUSLIBROS = booleanPreferencesKey("synopsis_todostuslibros")
        val SYNOPSIS_GOOGLE_BOOKS = booleanPreferencesKey("synopsis_google_books")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_DARK_THEME] ?: false
    }

    val accentColorIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_INDEX] ?: 0
    }

    val synopsisSources: Flow<SynopsisSourceConfig> = combine(
        context.dataStore.data.map { it[SYNOPSIS_ISBN] ?: true },
        context.dataStore.data.map { it[SYNOPSIS_CASA_DEL_LIBRO] ?: true },
        context.dataStore.data.map { it[SYNOPSIS_OPENLIBRARY] ?: true },
        context.dataStore.data.map { it[SYNOPSIS_WIKIPEDIA] ?: true },
        context.dataStore.data.map { it[SYNOPSIS_TODOSTUSLIBROS] ?: true },
        context.dataStore.data.map { it[SYNOPSIS_GOOGLE_BOOKS] ?: true }
    ) { arr ->
        SynopsisSourceConfig(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = enabled
        }
    }

    suspend fun setAccentColorIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[ACCENT_COLOR_INDEX] = index
        }
    }

    suspend fun setSynopsisSourceEnabled(source: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (source) {
                "isbn" -> prefs[SYNOPSIS_ISBN] = enabled
                "casa_del_libro" -> prefs[SYNOPSIS_CASA_DEL_LIBRO] = enabled
                "openlibrary" -> prefs[SYNOPSIS_OPENLIBRARY] = enabled
                "wikipedia" -> prefs[SYNOPSIS_WIKIPEDIA] = enabled
                "todostuslibros" -> prefs[SYNOPSIS_TODOSTUSLIBROS] = enabled
                "google_books" -> prefs[SYNOPSIS_GOOGLE_BOOKS] = enabled
            }
        }
    }
}
