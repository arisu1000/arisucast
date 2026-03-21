package com.arisucast.feature.subscriptions.domain

import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.model.Podcast
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.database.entity.PodcastEntity
import com.arisucast.core.database.entity.SubscriptionEntity
import com.arisucast.core.database.mapper.toDomainModel
import com.arisucast.core.network.rss.RssParser
import java.time.Instant
import javax.inject.Inject

class SubscribeToFeedUseCase @Inject constructor(
    private val rssParser: RssParser,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val subscriptionDao: SubscriptionDao
) {
    suspend operator fun invoke(feedUrl: String): Result<Podcast> {
        val trimmedUrl = feedUrl.trim()

        // Already subscribed check
        val podcastId = trimmedUrl.sha256()
        if (subscriptionDao.isSubscribed(podcastId)) {
            val existing = podcastDao.getById(podcastId)
            if (existing != null) {
                return Result.Success(existing.toDomainModel())
            }
        }

        return when (val parsed = rssParser.parseFeed(trimmedUrl)) {
            is Result.Error -> parsed
            is Result.Loading -> parsed
            is Result.Success -> {
                val feed = parsed.data
                val now = Instant.now()

                val podcast = PodcastEntity(
                    id = podcastId,
                    title = feed.title,
                    author = feed.author,
                    description = feed.description,
                    feedUrl = trimmedUrl,
                    imageUrl = feed.imageUrl,
                    websiteUrl = feed.websiteUrl,
                    language = feed.language,
                    category = feed.category,
                    lastUpdated = now.toEpochMilli(),
                    isSubscribed = true
                )
                podcastDao.upsert(podcast)

                subscriptionDao.insert(
                    SubscriptionEntity(
                        podcastId = podcastId,
                        feedUrl = trimmedUrl,
                        subscribedAt = now.toEpochMilli(),
                        autoDownload = false,
                        lastRefreshedAt = now.toEpochMilli()
                    )
                )

                val episodes = feed.episodes.map { ep ->
                    EpisodeEntity(
                        id = ep.guid.sha256(),
                        podcastId = podcastId,
                        title = ep.title,
                        description = ep.description,
                        audioUrl = ep.audioUrl,
                        imageUrl = ep.imageUrl.ifBlank { feed.imageUrl },
                        publishedAt = ep.publishedAt.toEpochMilli(),
                        durationSeconds = ep.durationSeconds,
                        fileSizeBytes = ep.fileSizeBytes,
                        mimeType = ep.mimeType,
                        season = ep.season ?: 0,
                        episode = ep.episodeNumber ?: 0,
                        playbackPositionMs = 0L,
                        isPlayed = false,
                        downloadStatus = "NONE",
                        localFilePath = null,
                        downloadWorkerId = null,
                        downloadedAt = null
                    )
                }
                episodeDao.insertAll(episodes)

                Result.Success(podcast.toDomainModel())
            }
        }
    }
}
