package com.arisucast.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arisucast.core.common.extensions.sha256
import com.arisucast.core.common.result.Result
import com.arisucast.core.database.dao.EpisodeDao
import com.arisucast.core.database.dao.PodcastDao
import com.arisucast.core.database.dao.SubscriptionDao
import com.arisucast.core.database.entity.EpisodeEntity
import com.arisucast.core.network.rss.RssParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant

@HiltWorker
class RefreshFeedsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rssParser: RssParser,
    private val subscriptionDao: SubscriptionDao,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val subscriptions = subscriptionDao.getAllSubscriptions().first()
        var hasError = false

        for (subscription in subscriptions) {
            val parseResult = rssParser.parseFeed(subscription.feedUrl)
            if (parseResult is com.arisucast.core.common.result.Result.Success) {
                val feed = parseResult.data
                val now = Instant.now()

                podcastDao.getById(subscription.podcastId)?.let { existing ->
                    // upsert(REPLACE) 대신 update 사용: REPLACE는 행을 DELETE 후 INSERT하므로
                    // ForeignKey(onDelete=CASCADE)에 의해 에피소드 전체가 삭제됨
                    podcastDao.update(
                        existing.copy(
                            title = feed.title,
                            author = feed.author,
                            imageUrl = feed.imageUrl.ifBlank { existing.imageUrl },
                            lastUpdated = now.toEpochMilli()
                        )
                    )
                }

                val episodes = feed.episodes.map { ep ->
                    EpisodeEntity(
                        id = ep.guid.sha256(),
                        podcastId = subscription.podcastId,
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
                subscriptionDao.updateLastRefreshed(subscription.podcastId, now.toEpochMilli())
            } else {
                hasError = true
            }
        }

        // 일부 피드 실패 시에도 retry하지 않음: retry는 지수 백오프로 모든 피드를 재요청하므로
        // 이미 성공한 피드까지 불필요하게 재처리됨. 실패한 피드는 다음 주기(6시간)에 재시도.
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "refresh_feeds"
    }
}
