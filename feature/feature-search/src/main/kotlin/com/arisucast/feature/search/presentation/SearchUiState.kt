package com.arisucast.feature.search.presentation

import com.arisucast.core.common.model.Podcast

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<Podcast>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
