package com.arisucast.feature.subscriptions.domain

import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.PodcastEntity
import com.arisucast.core.network.rss.ParsedEpisode
import com.arisucast.core.network.rss.ParsedPodcast
import com.arisucast.core.network.rss.RssParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SubscribeToFeedUseCaseTest {

    private val rssParser: RssParser = mockk()
    private val podcastDao: PodcastDao = mockk(relaxed = true)
    private val episodeDao: EpisodeDao = mockk(relaxed = true)
    private val subscriptionDao: SubscriptionDao = mockk(relaxed = true)

    private lateinit var useCase: SubscribeToFeedUseCase

    private val feedUrl = "https://example.com/podcast.rss"
    private val podcastId = feedUrl.sha256()

    private val fakeParsedPodcast = ParsedPodcast(
        title = "Test Podcast",
        author = "Test Author",
        description = "Test Description",
        imageUrl = "https://example.com/art.jpg",
        websiteUrl = "https://example.com",
        language = "ko",
        category = "Technology",
        episodes = listOf(
            ParsedEpisode(
                guid = "episode-1",
                title = "Episode 1",
                description = "Ep 1 desc",
                audioUrl = "https://example.com/ep1.mp3",
                imageUrl = "https://example.com/ep1.jpg",
                publishedAt = Instant.ofEpochSecond(1_700_000_000),
                durationSeconds = 3600,
                fileSizeBytes = 50_000_000L,
                mimeType = "audio/mpeg",
                season = null,
                episodeNumber = 1
            )
        )
    )

    @Before
    fun setUp() {
        useCase = SubscribeToFeedUseCase(rssParser, podcastDao, episodeDao, subscriptionDao)
    }

    @Test
    fun `success - valid feed saves podcast and episodes to DB`() = runTest {
        coEvery { subscriptionDao.isSubscribed(podcastId) } returns false
        coEvery { rssParser.parseFeed(feedUrl) } returns Result.Success(fakeParsedPodcast)

        val result = useCase(feedUrl)

        assertTrue(result is Result.Success)
        val podcast = (result as Result.Success).data
        assertEquals("Test Podcast", podcast.title)
        assertEquals(feedUrl, podcast.feedUrl)
        assertTrue(podcast.isSubscribed)

        // Verify DB writes
        coVerify { podcastDao.upsert(any()) }
        coVerify { subscriptionDao.insert(any()) }
        coVerify { episodeDao.insertAll(any()) }
    }

    @Test
    fun `already subscribed - returns existing podcast without re-parsing`() = runTest {
        val existingEntity = PodcastEntity(
            id = podcastId,
            title = "Cached Podcast",
            author = "Author",
            description = "Desc",
            feedUrl = feedUrl,
            imageUrl = "https://example.com/art.jpg",
            websiteUrl = "",
            language = "ko",
            category = "",
            lastUpdated = Instant.now().toEpochMilli(),
            isSubscribed = true
        )
        coEvery { subscriptionDao.isSubscribed(podcastId) } returns true
        coEvery { podcastDao.getById(podcastId) } returns existingEntity

        val result = useCase(feedUrl)

        assertTrue(result is Result.Success)
        assertEquals("Cached Podcast", (result as Result.Success).data.title)

        // Should NOT parse the feed again
        coVerify(exactly = 0) { rssParser.parseFeed(any()) }
    }

    @Test
    fun `rss parser error - propagates Error result`() = runTest {
        val error = Result.Error(Exception("Network error"), "피드를 불러올 수 없습니다.")
        coEvery { subscriptionDao.isSubscribed(podcastId) } returns false
        coEvery { rssParser.parseFeed(feedUrl) } returns error

        val result = useCase(feedUrl)

        assertTrue(result is Result.Error)

        // DB should not be written on error
        coVerify(exactly = 0) { podcastDao.upsert(any()) }
        coVerify(exactly = 0) { subscriptionDao.insert(any()) }
    }

    @Test
    fun `whitespace url is trimmed before processing`() = runTest {
        val urlWithSpaces = "  $feedUrl  "
        coEvery { subscriptionDao.isSubscribed(podcastId) } returns false
        coEvery { rssParser.parseFeed(feedUrl) } returns Result.Success(fakeParsedPodcast)

        useCase(urlWithSpaces)

        coVerify { rssParser.parseFeed(feedUrl) }
    }
}
