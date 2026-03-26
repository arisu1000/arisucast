package com.arisucast.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.model.Podcast
import com.arisucast.core.common.model.PodcastSortOrder
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.mapper.toDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(PodcastSortOrder.NAME_ASC)

    val uiState: StateFlow<HomeUiState> = combine(
        podcastDao.getSubscribedPodcasts(),
        episodeDao.getRecentEpisodes(50),
        _sortOrder
    ) { podcasts, episodes, sortOrder ->
        HomeUiState.Success(
            subscriptions = podcasts.map { it.toDomainModel() }.sorted(sortOrder),
            recentEpisodes = episodes.map { it.toDomainModel() },
            sortOrder = sortOrder
        ) as HomeUiState
    }.catch { e ->
        emit(HomeUiState.Error(e.message ?: "알 수 없는 오류"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    fun setSortOrder(order: PodcastSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(podcastId: String, currentValue: Boolean) {
        viewModelScope.launch {
            podcastDao.updateFavorite(podcastId, !currentValue)
        }
    }

    fun refresh() {
        // Feed refresh will be triggered via WorkManager
    }

    private fun List<Podcast>.sorted(order: PodcastSortOrder): List<Podcast> = when (order) {
        PodcastSortOrder.NAME_ASC -> sortedBy { it.title.lowercase() }
        PodcastSortOrder.LAST_UPDATED -> sortedByDescending { it.lastUpdated }
        PodcastSortOrder.FAVORITES_FIRST -> sortedWith(
            compareByDescending<Podcast> { it.isFavorite }.thenBy { it.title.lowercase() }
        )
    }
}
