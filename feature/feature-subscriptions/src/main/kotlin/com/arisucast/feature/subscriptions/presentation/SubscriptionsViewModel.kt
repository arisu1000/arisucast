package com.arisucast.feature.subscriptions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.model.PodcastSortOrder
import com.arisucast.core.common.model.sortedByOrder
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.mapper.toDomainModel
import com.arisucast.feature.subscriptions.domain.SubscribeToFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscribeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val podcastDao: PodcastDao,
    private val subscribeToFeedUseCase: SubscribeToFeedUseCase
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(PodcastSortOrder.NAME_ASC)

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        podcastDao.getSubscribedPodcasts(),
        _sortOrder
    ) { podcasts, sortOrder ->
        SubscriptionsUiState.Success(
            subscriptions = podcasts.map { it.toDomainModel() }.sortedByOrder(sortOrder),
            sortOrder = sortOrder
        ) as SubscriptionsUiState
    }
        .catch { e ->
            emit(SubscriptionsUiState.Error(e.message ?: "오류가 발생했습니다."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SubscriptionsUiState.Loading
        )

    fun setSortOrder(order: PodcastSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(podcastId: String, currentValue: Boolean) {
        viewModelScope.launch {
            podcastDao.updateFavorite(podcastId, !currentValue)
        }
    }

    private val _subscribeState = MutableStateFlow(SubscribeState())
    val subscribeState: StateFlow<SubscribeState> = _subscribeState

    fun subscribeToFeed(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _subscribeState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = subscribeToFeedUseCase(url)) {
                is Result.Success -> {
                    _subscribeState.update {
                        it.copy(isLoading = false, successMessage = "'${result.data.title}' 구독 완료!")
                    }
                }
                is Result.Error -> {
                    _subscribeState.update {
                        it.copy(isLoading = false, errorMessage = result.message ?: "구독에 실패했습니다.")
                    }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _subscribeState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
