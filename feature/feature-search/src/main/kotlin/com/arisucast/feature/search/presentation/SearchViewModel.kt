package com.arisucast.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.model.Podcast
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.database.entity.PodcastEntity
import com.arisucast.core.database.entity.SubscriptionEntity
import com.arisucast.core.network.api.ItunesSearchApi
import com.arisucast.core.network.rss.RssParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val itunesSearchApi: ItunesSearchApi,
    private val rssParser: RssParser,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val subscriptionDao: SubscriptionDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Tracks which podcast IDs are currently being subscribed
    private val _subscribingIds = MutableStateFlow<Set<String>>(emptySet())
    val subscribingIds: StateFlow<Set<String>> = _subscribingIds.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(400L) // debounce
            doSearch(newQuery)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { doSearch(query) }
    }

    fun subscribe(podcast: Podcast) {
        if (podcast.feedUrl.isBlank()) return
        viewModelScope.launch {
            _subscribingIds.update { it + podcast.id }
            try {
                val feedUrl = podcast.feedUrl
                val podcastId = feedUrl.sha256()

                when (val parsed = rssParser.parseFeed(feedUrl)) {
                    is Result.Success -> {
                        val feed = parsed.data
                        val now = Instant.now()

                        podcastDao.upsert(
                            PodcastEntity(
                                id = podcastId,
                                title = feed.title,
                                author = feed.author,
                                description = feed.description,
                                feedUrl = feedUrl,
                                imageUrl = feed.imageUrl,
                                websiteUrl = feed.websiteUrl,
                                language = feed.language,
                                category = feed.category,
                                lastUpdated = now.toEpochMilli(),
                                isSubscribed = true
                            )
                        )

                        subscriptionDao.insert(
                            SubscriptionEntity(
                                podcastId = podcastId,
                                feedUrl = feedUrl,
                                subscribedAt = now.toEpochMilli(),
                                autoDownload = false,
                                lastRefreshedAt = now.toEpochMilli()
                            )
                        )

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

                        // Update the result list to reflect subscribed state
                        val current = _uiState.value
                        if (current is SearchUiState.Success) {
                            _uiState.value = current.copy(
                                results = current.results.map {
                                    if (it.id == podcast.id) it.copy(isSubscribed = true) else it
                                }
                            )
                        }
                    }
                    is Result.Error -> { /* silent — user can retry */ }
                    is Result.Loading -> { /* no-op */ }
                }
            } finally {
                _subscribingIds.update { it - podcast.id }
            }
        }
    }

    private suspend fun doSearch(query: String) {
        _uiState.value = SearchUiState.Loading
        try {
            val response = itunesSearchApi.searchPodcasts(query.trim())
            val podcasts = response.results
                .filter { it.feedUrl.isNotBlank() }
                .map { dto ->
                    Podcast(
                        id = dto.feedUrl.sha256(),
                        title = dto.trackName,
                        feedUrl = dto.feedUrl,
                        imageUrl = dto.artworkUrl,
                        author = dto.artistName,
                        description = dto.genre,
                        websiteUrl = "",
                        category = dto.genre,
                        language = "",
                        isSubscribed = false,
                        isFavorite = false,
                        lastUpdated = Instant.EPOCH
                    )
                }
            _uiState.value = SearchUiState.Success(podcasts)
        } catch (e: Exception) {
            _uiState.value = SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다.")
        }
    }
}
