package com.arisucast.core.common.model

data class PlaybackState(
    val currentEpisode: Episode? = null,
    val currentPodcastTitle: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val bufferedPositionMs: Long = 0L,
    val playerState: PlayerState = PlayerState.IDLE,
    val sleepTimerEndMs: Long = 0L  // 0 = no timer active
) {
    val progress: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    val sleepTimerActive: Boolean get() = sleepTimerEndMs > 0L
}

enum class PlayerState {
    IDLE, BUFFERING, READY, ENDED
}
