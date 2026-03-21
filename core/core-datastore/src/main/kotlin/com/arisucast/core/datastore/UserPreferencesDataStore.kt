package com.arisucast.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val wifiOnlyDownload: Boolean = true,
    val autoDownload: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val darkTheme: Boolean = false
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            wifiOnlyDownload = prefs[Keys.WIFI_ONLY_DOWNLOAD] ?: true,
            autoDownload = prefs[Keys.AUTO_DOWNLOAD] ?: false,
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            darkTheme = prefs[Keys.DARK_THEME] ?: false
        )
    }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        dataStore.edit { it[Keys.WIFI_ONLY_DOWNLOAD] = enabled }
    }

    suspend fun setAutoDownload(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_DOWNLOAD] = enabled }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        dataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = speed }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }
}
