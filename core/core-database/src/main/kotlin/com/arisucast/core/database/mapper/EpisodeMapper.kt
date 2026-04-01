package com.arisucast.core.database.mapper

import com.arisucast.core.common.model.DownloadState
import com.arisucast.core.common.model.Episode
import com.arisucast.core.database.entity.EpisodeEntity
import java.time.Instant
import java.util.UUID

fun EpisodeEntity.toDomainModel(): Episode = Episode(
    id = id,
    podcastId = podcastId,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    publishedAt = Instant.ofEpochMilli(publishedAt),
    durationSeconds = durationSeconds,
    fileSizeBytes = fileSizeBytes,
    mimeType = mimeType,
    season = season.takeIf { it > 0 },
    episode = episode.takeIf { it > 0 },
    playbackPositionMs = playbackPositionMs,
    isPlayed = isPlayed,
    downloadState = toDownloadState()
)

private fun EpisodeEntity.toDownloadState(): DownloadState = when (downloadStatus) {
    "DOWNLOADING" -> DownloadState.Downloading(
        progressPercent = 0,
        workerId = downloadWorkerId?.let { UUID.fromString(it) } ?: UUID.randomUUID()
    )
    "DONE" -> if (!localFilePath.isNullOrBlank()) {
        DownloadState.Downloaded(
            localFilePath = localFilePath,
            downloadedAt = Instant.ofEpochMilli(downloadedAt ?: 0L)
        )
    } else {
        // localFilePath 누락 시 빈 문자열로 재생 시도하면 ExoPlayer 오류 발생 → NotDownloaded 폴백
        DownloadState.NotDownloaded
    }
    "FAILED" -> DownloadState.Failed("Download failed")
    else -> DownloadState.NotDownloaded
}

fun Episode.toEntity(): EpisodeEntity = EpisodeEntity(
    id = id,
    podcastId = podcastId,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    publishedAt = publishedAt.toEpochMilli(),
    durationSeconds = durationSeconds,
    fileSizeBytes = fileSizeBytes,
    mimeType = mimeType,
    season = season ?: 0,
    episode = episode ?: 0,
    playbackPositionMs = playbackPositionMs,
    isPlayed = isPlayed,
    downloadStatus = downloadState.toStatusString(),
    localFilePath = (downloadState as? DownloadState.Downloaded)?.localFilePath,
    downloadWorkerId = (downloadState as? DownloadState.Downloading)?.workerId?.toString(),
    downloadedAt = (downloadState as? DownloadState.Downloaded)?.downloadedAt?.toEpochMilli()
)

private fun DownloadState.toStatusString(): String = when (this) {
    is DownloadState.NotDownloaded -> "NONE"
    is DownloadState.Downloading -> "DOWNLOADING"
    is DownloadState.Downloaded -> "DONE"
    is DownloadState.Failed -> "FAILED"
}
