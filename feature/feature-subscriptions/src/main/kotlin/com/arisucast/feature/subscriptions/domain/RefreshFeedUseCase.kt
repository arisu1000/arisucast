package com.arisucast.feature.subscriptions.domain

import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.network.rss.RssParser
import java.time.Instant
import javax.inject.Inject

class RefreshFeedUseCase @Inject constructor(
    private val rssParser: RssParser,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val subscriptionDao: SubscriptionDao
) {
    suspend operator fun invoke(podcastId: String): Result<Unit> {
        val subscription = subscriptionDao.getById(podcastId)
            ?: return Result.Error(Exception("구독 정보를 찾을 수 없습니다."))

        return when (val parsed = rssParser.parseFeed(subscription.feedUrl)) {
            is Result.Error -> parsed
            is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
            is Result.Success -> {
                val feed = parsed.data
                val now = Instant.now()

                // Update podcast metadata
                podcastDao.getById(podcastId)?.let { existing ->
                    podcastDao.upsert(
                        existing.copy(
                            title = feed.title,
                            author = feed.author,
                            description = feed.description,
                            imageUrl = feed.imageUrl.ifBlank { existing.imageUrl },
                            lastUpdated = now.toEpochMilli()
                        )
                    )
                }

                // Insert new episodes (IGNORE conflict preserves existing playback state)
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
                episodeDao.insertAll(episodes) // IGNORE = preserves existing playback state

                subscriptionDao.updateLastRefreshed(podcastId, now.toEpochMilli())

                Result.Success(Unit)
            }
        }
    }
}
