package com.arisucast.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arisucast.core.common.model.Episode
import com.arisucast.core.common.model.PlayerState
import com.arisucast.core.common.model.PlaybackState
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.media.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
    private val episodeDao: EpisodeDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionSaveJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // 재생 완료 시 DB 위치를 0으로 초기화 → 다음 재생 시 처음부터 시작
                    player.currentMediaItem?.mediaId?.let { episodeId ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                episodeDao.updatePlaybackPosition(episodeId, 0L)
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(
                        playerState = when (playbackState) {
                            Player.STATE_IDLE -> PlayerState.IDLE
                            Player.STATE_BUFFERING -> PlayerState.BUFFERING
                            Player.STATE_READY -> PlayerState.READY
                            Player.STATE_ENDED -> PlayerState.ENDED
                            else -> PlayerState.IDLE
                        },
                        durationMs = player.duration.takeIf { d -> d > 0 } ?: 0L
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // 에러 상태를 IDLE로 전환하여 다음 playEpisode 호출이 정상 작동하도록 함
                player.stop()
                _state.update { it.copy(playerState = PlayerState.IDLE, isPlaying = false) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) {
                    _state.update {
                        it.copy(
                            currentEpisode = null,
                            currentPodcastTitle = null,
                            durationMs = 0L,
                            positionMs = 0L,
                            bufferedPositionMs = 0L
                        )
                    }
                }
            }
        })
    }

    fun playEpisode(episode: Episode, podcastTitle: String) {
        // Start PlaybackService so the system shows a media notification
        val serviceIntent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        val audioUrl = (episode.downloadState as? com.arisucast.core.common.model.DownloadState.Downloaded)?.localFilePath
            ?: episode.audioUrl

        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(podcastTitle)
                    .setArtworkUri(android.net.Uri.parse(episode.imageUrl))
                    .build()
            )
            .build()

        _state.update { 
            it.copy(
                currentEpisode = episode,
                currentPodcastTitle = podcastTitle
            )
        }

        // ENDED/에러 상태를 클리어하고 새 미디어 아이템 준비
        player.stop()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (episode.playbackPositionMs > 0) player.seekTo(episode.playbackPositionMs)
        player.play()
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun seekToFraction(fraction: Float) {
        val duration = player.duration.takeIf { it > 0 } ?: return
        seekTo((duration * fraction).toLong())
    }

    fun skipBack(ms: Long = 10_000L) {
        seekTo(maxOf(0L, player.currentPosition - ms))
    }

    fun skipForward(ms: Long = 30_000L) {
        val duration = player.duration.takeIf { it > 0 } ?: return
        seekTo(minOf(duration, player.currentPosition + ms))
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun currentEpisodeId(): String? = player.currentMediaItem?.mediaId

    /** Set a sleep timer to pause playback after [durationMs] milliseconds. Pass 0 to cancel. */
    fun setSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _state.update { it.copy(sleepTimerEndMs = 0L) }
        if (durationMs <= 0L) return
        val endMs = System.currentTimeMillis() + durationMs
        _state.update { it.copy(sleepTimerEndMs = endMs) }
        sleepTimerJob = scope.launch {
            delay(durationMs)
            if (player.isPlaying) player.pause()
            _state.update { it.copy(sleepTimerEndMs = 0L) }
        }
    }

    private fun startPositionUpdates() {
        positionSaveJob?.cancel()
        positionSaveJob = scope.launch {
            while (isActive) {
                val position = player.currentPosition
                val duration = player.duration.takeIf { it > 0 } ?: 0L
                val buffered = player.bufferedPosition

                _state.update {
                    it.copy(
                        positionMs = position,
                        durationMs = duration,
                        bufferedPositionMs = buffered
                    )
                }

                // Save position to DB every 5 seconds
                player.currentMediaItem?.mediaId?.let { episodeId ->
                    if (episodeId.isNotBlank()) {
                        episodeDao.updatePlaybackPosition(episodeId, position)
                    }
                }

                delay(1_000L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionSaveJob?.cancel()
    }
}
