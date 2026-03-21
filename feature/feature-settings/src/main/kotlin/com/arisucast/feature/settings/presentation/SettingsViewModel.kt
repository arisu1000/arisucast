package com.arisucast.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = dataStore.preferences
        .map { prefs ->
            SettingsUiState(
                wifiOnlyDownload = prefs.wifiOnlyDownload,
                autoDownload = prefs.autoDownload,
                defaultPlaybackSpeed = prefs.defaultPlaybackSpeed,
                darkTheme = prefs.darkTheme
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setWifiOnlyDownload(enabled: Boolean) {
        viewModelScope.launch { dataStore.setWifiOnlyDownload(enabled) }
    }

    fun setAutoDownload(enabled: Boolean) {
        viewModelScope.launch { dataStore.setAutoDownload(enabled) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { dataStore.setDefaultPlaybackSpeed(speed) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDarkTheme(enabled) }
    }
}
