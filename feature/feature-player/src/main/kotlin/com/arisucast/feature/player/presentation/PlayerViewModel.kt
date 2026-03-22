package com.arisucast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.mapper.toDomainModel
import com.arisucast.core.media.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playbackRepository.state
        .map { playbackState ->
            val episode = playbackState.currentEpisode ?: return@map PlayerUiState.NothingPlaying

            PlayerUiState.Playing(
                episodeId = episode.id,
                episodeTitle = episode.title,
                podcastTitle = playbackState.currentPodcastTitle ?: "",
                imageUrl = episode.imageUrl,
                isPlaying = playbackState.isPlaying,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                playbackSpeed = playbackState.playbackSpeed,
                progress = playbackState.progress,
                sleepTimerEndMs = playbackState.sleepTimerEndMs
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerUiState.NothingPlaying
        )

    fun togglePlayPause() {
        playbackRepository.playPause()
    }

    fun seekTo(progress: Float) {
        playbackRepository.seekToFraction(progress)
    }

    fun skipBack10() {
        playbackRepository.skipBack(10_000L)
    }

    fun skipForward30() {
        playbackRepository.skipForward(30_000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackRepository.setPlaybackSpeed(speed)
    }

    /** Set sleep timer. [minutes] == 0 cancels the timer. */
    fun setSleepTimer(minutes: Int) {
        playbackRepository.setSleepTimer(minutes * 60_000L)
    }
}
