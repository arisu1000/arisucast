package com.arisucast.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisucast.core.common.model.PlaybackState
import com.arisucast.core.datastore.UserPreferencesDataStore
import com.arisucast.core.media.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val dataStore: UserPreferencesDataStore
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackRepository.state

    val darkTheme: StateFlow<Boolean> = dataStore.preferences
        .map { it.darkTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun togglePlayPause() {
        playbackRepository.playPause()
    }
}
