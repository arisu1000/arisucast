package com.arisucast.feature.episodes.presentation

import com.arisucast.core.common.model.Episode

sealed class EpisodeListUiState {
    object Loading : EpisodeListUiState()
    data class Success(
        val podcastTitle: String,
        val episodes: List<Episode>,
        val currentEpisodeId: String? = null,
        val isPlaying: Boolean = false
    ) : EpisodeListUiState()
    data class Error(val message: String) : EpisodeListUiState()
}
