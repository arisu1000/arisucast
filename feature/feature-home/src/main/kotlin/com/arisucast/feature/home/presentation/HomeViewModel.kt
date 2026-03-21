package com.arisucast.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.mapper.toDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        podcastDao.getSubscribedPodcasts(),
        episodeDao.getRecentEpisodes(50)
    ) { podcasts, episodes ->
        HomeUiState.Success(
            subscriptions = podcasts.map { it.toDomainModel() },
            recentEpisodes = episodes.map { it.toDomainModel() }
        ) as HomeUiState
    }.catch { e ->
        emit(HomeUiState.Error(e.message ?: "알 수 없는 오류"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    fun refresh() {
        // Feed refresh will be triggered via WorkManager
    }
}
