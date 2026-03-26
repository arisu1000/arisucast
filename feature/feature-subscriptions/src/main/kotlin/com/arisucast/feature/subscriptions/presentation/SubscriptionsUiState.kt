package com.arisucast.feature.subscriptions.presentation

import com.arisucast.core.common.model.Podcast
import com.arisucast.core.common.model.PodcastSortOrder

sealed class SubscriptionsUiState {
    object Loading : SubscriptionsUiState()
    data class Success(
        val subscriptions: List<Podcast>,
        val sortOrder: PodcastSortOrder = PodcastSortOrder.NAME_ASC
    ) : SubscriptionsUiState()
    data class Error(val message: String) : SubscriptionsUiState()
}
