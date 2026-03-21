package com.arisucast.feature.episodes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.model.DownloadState
import com.arisucast.core.common.model.Episode
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.mapper.toDomainModel
import com.arisucast.core.download.manager.DownloadManager
import com.arisucast.core.media.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val playbackRepository: PlaybackRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    val uiState: StateFlow<EpisodeListUiState> = combine(
        podcastDao.getSubscribedPodcasts(),
        episodeDao.getEpisodesByPodcast(podcastId),
        playbackRepository.state
    ) { podcasts, episodes, playbackState ->
        val podcast = podcasts.find { it.id == podcastId }
        EpisodeListUiState.Success(
            podcastTitle = podcast?.title ?: "",
            episodes = episodes.map { it.toDomainModel() },
            currentEpisodeId = playbackState.currentEpisode?.id,
            isPlaying = playbackState.isPlaying
        ) as EpisodeListUiState
    }.catch { e ->
        emit(EpisodeListUiState.Error(e.message ?: "오류가 발생했습니다."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EpisodeListUiState.Loading
    )

    fun playEpisode(episode: Episode) {
        playbackRepository.playEpisode(
            episodeId = episode.id,
            audioUrl = (episode.downloadState as? DownloadState.Downloaded)?.localFilePath
                ?: episode.audioUrl,
            title = episode.title,
            artworkUrl = episode.imageUrl,
            startPositionMs = episode.playbackPositionMs
        )
    }

    fun toggleDownload(episode: Episode) {
        when (episode.downloadState) {
            is DownloadState.NotDownloaded, is DownloadState.Failed -> {
                downloadManager.downloadEpisode(
                    episodeId = episode.id,
                    audioUrl = episode.audioUrl,
                    episodeTitle = episode.title
                )
            }
            is DownloadState.Downloading -> {
                downloadManager.cancelDownload(episode.id)
            }
            is DownloadState.Downloaded -> {
                // Already downloaded — do nothing (delete handled in Settings)
            }
        }
    }
}
