package com.arisucast.feature.subscriptions.presentation

import com.arisucast.core.common.model.Podcast

sealed class SubscriptionsUiState {
    object Loading : SubscriptionsUiState()
    data class Success(val subscriptions: List<Podcast>) : SubscriptionsUiState()
    data class Error(val message: String) : SubscriptionsUiState()
}
