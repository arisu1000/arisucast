package com.arisucast.feature.episodes.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arisucast.core.common.model.DownloadState
import com.arisucast.core.ui.component.EpisodeItem
import com.arisucast.core.ui.component.ErrorMessage
import com.arisucast.core.ui.component.LoadingIndicator
import com.arisucast.core.ui.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    podcastId: String,
    onNavigateUp: () -> Unit,
    onPlayerClick: () -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? EpisodeListUiState.Success)?.podcastTitle ?: "에피소드") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is EpisodeListUiState.Loading -> LoadingIndicator()
            is EpisodeListUiState.Error -> ErrorMessage(message = state.message)
            is EpisodeListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(state.episodes, key = { it.id }) { episode ->
                        val isCurrentEpisode = episode.id == state.currentEpisodeId
                        EpisodeItem(
                            title = episode.title,
                            podcastTitle = state.podcastTitle,
                            imageUrl = episode.imageUrl,
                            durationText = formatDuration(episode.durationSeconds),
                            playbackProgress = if (episode.durationSeconds > 0) {
                                (episode.playbackPositionMs / 1000f) / episode.durationSeconds
                            } else 0f,
                            isDownloaded = episode.downloadState is DownloadState.Downloaded,
                            isDownloading = episode.downloadState is DownloadState.Downloading,
                            downloadProgress = (episode.downloadState as? DownloadState.Downloading)
                                ?.progressPercent?.toFloat()?.div(100f) ?: 0f,
                            isCurrentlyPlaying = isCurrentEpisode && state.isPlaying,
                            onPlayClick = {
                                viewModel.playEpisode(episode)
                                onPlayerClick()
                            },
                            onDownloadClick = { viewModel.toggleDownload(episode) },
                            onClick = {}
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
