package com.arisucast.feature.subscriptions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arisucast.core.ui.component.ErrorMessage
import com.arisucast.core.ui.component.LoadingIndicator
import com.arisucast.core.ui.component.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onPodcastClick: (podcastId: String) -> Unit,
    deepLinkFeedUrl: String? = null,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscribeState by viewModel.subscribeState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-subscribe when launched via deep link
    LaunchedEffect(deepLinkFeedUrl) {
        if (!deepLinkFeedUrl.isNullOrBlank()) {
            viewModel.subscribeToFeed(deepLinkFeedUrl)
        }
    }

    // Show snackbar on error or success
    LaunchedEffect(subscribeState.errorMessage) {
        subscribeState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(subscribeState.successMessage) {
        subscribeState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("라이브러리") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "구독 추가")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (subscribeState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (val state = uiState) {
                is SubscriptionsUiState.Loading -> LoadingIndicator()
                is SubscriptionsUiState.Error -> ErrorMessage(message = state.message)
                is SubscriptionsUiState.Success -> {
                    if (state.subscriptions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "구독 중인 팟캐스트가 없습니다.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "+ 버튼으로 RSS 피드를 추가하세요",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(padding)
                        ) {
                            items(state.subscriptions, key = { it.id }) { podcast ->
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
            }
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            isLoading = subscribeState.isLoading,
            onDismiss = { showAddDialog = false },
            onConfirm = { url ->
                viewModel.subscribeToFeed(url)
                showAddDialog = false
            }
        )
    }
}
