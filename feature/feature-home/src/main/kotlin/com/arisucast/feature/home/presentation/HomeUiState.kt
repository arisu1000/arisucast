package com.arisucast.feature.home.presentation

import com.arisucast.core.common.model.Episode
import com.arisucast.core.common.model.Podcast
import com.arisucast.core.common.model.PodcastSortOrder

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val subscriptions: List<Podcast>,
        val recentEpisodes: List<Episode>,
        val sortOrder: PodcastSortOrder = PodcastSortOrder.NAME_ASC
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
