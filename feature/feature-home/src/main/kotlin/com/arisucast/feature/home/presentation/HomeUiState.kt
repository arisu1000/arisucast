package com.arisucast.feature.home.presentation

import com.arisucast.core.common.model.Episode
import com.arisucast.core.common.model.Podcast

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val subscriptions: List<Podcast>,
        val recentEpisodes: List<Episode>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
