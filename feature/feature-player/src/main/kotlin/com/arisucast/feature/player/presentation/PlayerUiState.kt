package com.arisucast.feature.player.presentation

sealed class PlayerUiState {
    object NothingPlaying : PlayerUiState()
    data class Playing(
        val episodeId: String,
        val episodeTitle: String,
        val podcastTitle: String,
        val imageUrl: String,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val playbackSpeed: Float,
        val progress: Float,
        val sleepTimerEndMs: Long = 0L
    ) : PlayerUiState() {
        val sleepTimerActive: Boolean get() = sleepTimerEndMs > 0L
    }
}
