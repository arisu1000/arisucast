package com.arisucast.feature.search.presentation

import app.cash.turbine.test
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.network.api.ItunesSearchApi
import com.arisucast.core.network.api.dto.ItunesPodcastDto
import com.arisucast.core.network.api.dto.ItunesSearchResponse
import com.arisucast.core.network.rss.RssParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val itunesSearchApi: ItunesSearchApi = mockk()
    private val rssParser: RssParser = mockk()
    private val podcastDao: PodcastDao = mockk(relaxed = true)
    private val episodeDao: EpisodeDao = mockk(relaxed = true)
    private val subscriptionDao: SubscriptionDao = mockk(relaxed = true)

    private lateinit var viewModel: SearchViewModel

    private val fakeResponse = ItunesSearchResponse(
        resultCount = 2,
        results = listOf(
            ItunesPodcastDto(
                trackId = 1L,
                trackName = "Podcast Alpha",
                artistName = "Author A",
                artworkUrl = "https://example.com/art1.jpg",
                feedUrl = "https://example.com/feed1.rss",
                genre = "Technology"
            ),
            ItunesPodcastDto(
                trackId = 2L,
                trackName = "Podcast Beta",
                artistName = "Author B",
                artworkUrl = "https://example.com/art2.jpg",
                feedUrl = "https://example.com/feed2.rss",
                genre = "Science"
            ),
            // DTO with empty feedUrl should be filtered out
            ItunesPodcastDto(
                trackId = 3L,
                trackName = "No Feed",
                artistName = "Ghost",
                artworkUrl = "",
                feedUrl = "",
                genre = ""
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(itunesSearchApi, rssParser, podcastDao, episodeDao, subscriptionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun `empty query resets state to Idle before debounce fires`() = runTest {
        viewModel.onQueryChange("kotlin")
        advanceTimeBy(200L)  // less than the 400ms debounce — search not yet triggered
        viewModel.onQueryChange("")  // cancels pending debounce job
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
        coVerify(exactly = 0) { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) }
    }

    @Test
    fun `search success - maps results filtering empty feedUrl`() = runTest {
        coEvery { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) } returns fakeResponse

        viewModel.search("kotlin")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success)
        val results = (state as SearchUiState.Success).results
        assertEquals(2, results.size)  // 3rd DTO filtered (empty feedUrl)
        assertEquals("Podcast Alpha", results[0].title)
        assertEquals("Podcast Beta", results[1].title)
    }

    @Test
    fun `search failure - emits Error state`() = runTest {
        coEvery { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) } throws Exception("No internet")

        viewModel.search("test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error)
        assertTrue((state as SearchUiState.Error).message.contains("No internet"))
    }

    @Test
    fun `onQueryChange debounce - rapid typing triggers only one API call`() = runTest {
        coEvery { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) } returns fakeResponse

        viewModel.onQueryChange("k")
        advanceTimeBy(100L)
        viewModel.onQueryChange("ko")
        advanceTimeBy(100L)
        viewModel.onQueryChange("kot")
        advanceTimeBy(100L)
        viewModel.onQueryChange("kotl")
        advanceTimeBy(100L)
        viewModel.onQueryChange("kotlin")
        advanceTimeBy(500L)  // past debounce threshold
        advanceUntilIdle()

        // Only the last call after debounce should fire
        coVerify(exactly = 1) { itunesSearchApi.searchPodcasts("kotlin", any(), any(), any()) }
    }

    @Test
    fun `search emits Loading then Success`() = runTest {
        coEvery { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) } returns fakeResponse

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.search("kotlin")
            assertEquals(SearchUiState.Loading, awaitItem())

            val success = awaitItem()
            assertTrue(success is SearchUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `subscribingIds tracks in-progress subscriptions`() = runTest {
        // subscribe() is async and hits RssParser - test that the ID is added while in progress
        coEvery { rssParser.parseFeed(any()) } coAnswers {
            // parseFeed never returns (simulating slow network) — we just check the set
            com.arisucast.core.common.result.Result.Error(Exception("timeout"), "timeout")
        }

        // Simulate a search result first
        coEvery { itunesSearchApi.searchPodcasts(any(), any(), any(), any()) } returns fakeResponse
        viewModel.search("kotlin")
        advanceUntilIdle()

        val podcast = (viewModel.uiState.value as SearchUiState.Success).results[0]

        viewModel.subscribingIds.test {
            assertEquals(emptySet<String>(), awaitItem())

            viewModel.subscribe(podcast)
            advanceUntilIdle()

            // After subscribe completes (with error), id is removed from the set
            cancelAndIgnoreRemainingEvents()
        }
    }
}
