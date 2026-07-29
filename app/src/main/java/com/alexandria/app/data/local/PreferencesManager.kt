package com.alexandria.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alexandria.app.data.model.CoverSource
import com.alexandria.app.data.model.CoverSourceConfig
import com.alexandria.app.domain.model.VisualMode
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SynopsisSourceConfig(
    val enabledSources: List<String> = defaultOrder
) {
    fun isEnabled(key: String): Boolean = enabledSources.contains(key)

    fun toggleSource(key: String): SynopsisSourceConfig {
        return if (enabledSources.contains(key)) {
            copy(enabledSources = enabledSources.filter { it != key })
        } else {
            copy(enabledSources = enabledSources + key)
        }
    }

    fun moveSource(fromIndex: Int, toIndex: Int): SynopsisSourceConfig {
        val newList = enabledSources.toMutableList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)
        return copy(enabledSources = newList)
    }

    fun toJson(): String = Gson().toJson(this)

    companion object {
        val defaultOrder = listOf("isbn", "todostuslibros", "casa_del_libro", "openlibrary", "wikipedia", "google_books")

        fun fromJson(json: String): SynopsisSourceConfig =
            Gson().fromJson(json, SynopsisSourceConfig::class.java)
    }
}

class PreferencesManager(private val context: Context) {

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val SYNOPSIS_SOURCES_CONFIG = stringPreferencesKey("synopsis_sources_config")
        val COVER_SOURCES_CONFIG = stringPreferencesKey("cover_sources_config")
        val COVER_CACHE_ENABLED = booleanPreferencesKey("cover_cache_enabled")
        val VISUAL_MODE = stringPreferencesKey("visual_mode")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")

        fun getDefaultCoverConfig(): CoverSourceConfig = CoverSourceConfig()
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_DARK_THEME] ?: false
    }

    val accentColorIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_INDEX] ?: 0
    }

    val synopsisSources: Flow<SynopsisSourceConfig> = context.dataStore.data.map { prefs ->
        val json = prefs[SYNOPSIS_SOURCES_CONFIG]
        if (json != null && json.isNotBlank()) {
            try {
                SynopsisSourceConfig.fromJson(json)
            } catch (e: Exception) {
                SynopsisSourceConfig()
            }
        } else {
            SynopsisSourceConfig()
        }
    }

    val coverSourcesConfig: Flow<CoverSourceConfig> = context.dataStore.data.map { prefs ->
        val json = prefs[COVER_SOURCES_CONFIG]
        if (json != null && json.isNotBlank()) {
            try {
                Gson().fromJson(json, CoverSourceConfig::class.java)
            } catch (e: Exception) {
                CoverSourceConfig()
            }
        } else {
            CoverSourceConfig()
        }
    }

    val coverCacheEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COVER_CACHE_ENABLED] ?: true
    }

    val visualMode: Flow<VisualMode> = context.dataStore.data.map { prefs ->
        val name = prefs[VISUAL_MODE]
        try {
            name?.let { VisualMode.valueOf(it) } ?: VisualMode.CLASSIC
        } catch (e: Exception) {
            VisualMode.CLASSIC
        }
    }

    val firstLaunchCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FIRST_LAUNCH_COMPLETED] ?: false
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

    suspend fun setSynopsisSourcesConfig(config: SynopsisSourceConfig) {
        context.dataStore.edit { prefs ->
            prefs[SYNOPSIS_SOURCES_CONFIG] = config.toJson()
        }
    }

    suspend fun setCoverSourcesConfig(config: CoverSourceConfig) {
        context.dataStore.edit { prefs ->
            prefs[COVER_SOURCES_CONFIG] = Gson().toJson(config)
        }
    }

    suspend fun setCoverCacheEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[COVER_CACHE_ENABLED] = enabled
        }
    }

    suspend fun setVisualMode(mode: VisualMode) {
        context.dataStore.edit { prefs ->
            prefs[VISUAL_MODE] = mode.name
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH_COMPLETED] = true
        }
    }
}
