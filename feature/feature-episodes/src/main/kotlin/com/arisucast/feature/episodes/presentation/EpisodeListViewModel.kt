package com.arisucast.feature.episodes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.model.DownloadState
import com.arisucast.core.common.model.Episode
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.database.mapper.toDomainModel
import com.arisucast.core.download.manager.DownloadManager
import com.arisucast.core.media.PlaybackRepository
import com.arisucast.core.network.rss.RssParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val subscriptionDao: SubscriptionDao,
    private val rssParser: RssParser,
    private val playbackRepository: PlaybackRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    private val _isRefreshing = MutableStateFlow(false)

    // 갱신 실패 시 Snackbar 표시용 단발성 이벤트
    private val _refreshErrorChannel = Channel<String>(Channel.BUFFERED)
    val refreshErrors = _refreshErrorChannel.receiveAsFlow()

    val uiState: StateFlow<EpisodeListUiState> = combine(
        podcastDao.getSubscribedPodcasts(),
        episodeDao.getEpisodesByPodcast(podcastId),
        playbackRepository.state,
        _isRefreshing
    ) { podcasts, episodes, playbackState, isRefreshing ->
        val podcast = podcasts.find { it.id == podcastId }
        EpisodeListUiState.Success(
            podcastTitle = podcast?.title ?: "",
            episodes = episodes.map { it.toDomainModel() },
            currentEpisodeId = playbackState.currentEpisode?.id,
            isPlaying = playbackState.isPlaying,
            isRefreshing = isRefreshing
        ) as EpisodeListUiState
    }.catch { e ->
        emit(EpisodeListUiState.Error(e.message ?: "오류가 발생했습니다."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EpisodeListUiState.Loading
    )

    init {
        // 화면 진입 시 최신 에피소드를 자동으로 가져옴
        refresh()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.update { true }
            try {
                val subscription = subscriptionDao.getById(podcastId) ?: return@launch
                val result = rssParser.parseFeed(subscription.feedUrl)
                if (result is Result.Success) {
                    val feed = result.data
                    val now = Instant.now()

                    // upsert(REPLACE) 대신 update 사용: REPLACE는 행을 DELETE 후 INSERT하므로
                    // ForeignKey(onDelete=CASCADE)에 의해 에피소드 전체가 삭제됨
                    podcastDao.getById(podcastId)?.let { existing ->
                        podcastDao.update(
                            existing.copy(
                                title = feed.title,
                                author = feed.author,
                                description = feed.description,
                                imageUrl = feed.imageUrl.ifBlank { existing.imageUrl },
                                lastUpdated = now.toEpochMilli()
                            )
                        )
                    }

                    val episodes = feed.episodes.map { ep ->
                        EpisodeEntity(
                            id = ep.guid.sha256(),
                            podcastId = podcastId,
                            title = ep.title,
                            description = ep.description,
                            audioUrl = ep.audioUrl,
                            imageUrl = ep.imageUrl.ifBlank { feed.imageUrl },
                            publishedAt = ep.publishedAt.toEpochMilli(),
                            durationSeconds = ep.durationSeconds,
                            fileSizeBytes = ep.fileSizeBytes,
                            mimeType = ep.mimeType,
                            season = ep.season ?: 0,
                            episode = ep.episodeNumber ?: 0,
                            playbackPositionMs = 0L,
                            isPlayed = false,
                            downloadStatus = "NONE",
                            localFilePath = null,
                            downloadWorkerId = null,
                            downloadedAt = null
                        )
                    }
                    episodeDao.insertAll(episodes)
                    subscriptionDao.updateLastRefreshed(podcastId, now.toEpochMilli())
                } else if (result is Result.Error) {
                    _refreshErrorChannel.trySend("피드 갱신에 실패했습니다.")
                }
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    fun playEpisode(episode: Episode) {
        val podcastTitle = (uiState.value as? EpisodeListUiState.Success)?.podcastTitle ?: ""
        playbackRepository.playEpisode(episode, podcastTitle)
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
