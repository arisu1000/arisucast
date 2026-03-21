package com.arisucast.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arisucast.core.ui.component.ErrorMessage
import com.arisucast.core.ui.component.LoadingIndicator
import com.arisucast.core.ui.component.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPodcastClick: (podcastId: String) -> Unit,
    onPlayerClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("ArisuCast") })

        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingIndicator()
            is HomeUiState.Error -> ErrorMessage(
                message = state.message,
                onRetry = viewModel::refresh
            )
            is HomeUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.subscriptions.isNotEmpty()) {
                        item {
                            Text(
                                text = "구독 중",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.subscriptions) { podcast ->
                                    PodcastCard(
                                        title = podcast.title,
                                        author = podcast.author,
                                        imageUrl = podcast.imageUrl,
                                        onClick = { onPodcastClick(podcast.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (state.recentEpisodes.isNotEmpty()) {
                        item {
                            Text(
                                text = "최근 에피소드",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(state.recentEpisodes) { episode ->
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (state.subscriptions.isEmpty() && state.recentEpisodes.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "아직 구독한 팟캐스트가 없습니다.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "검색 탭에서 팟캐스트를 찾아보세요!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
