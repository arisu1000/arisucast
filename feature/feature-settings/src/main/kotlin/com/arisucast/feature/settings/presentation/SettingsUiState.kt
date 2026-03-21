package com.arisucast.feature.settings.presentation

data class SettingsUiState(
    val wifiOnlyDownload: Boolean = true,
    val autoDownload: Boolean = false,
    val defaultPlaybackSpeed: Float = 1.0f,
    val darkTheme: Boolean = false
)
