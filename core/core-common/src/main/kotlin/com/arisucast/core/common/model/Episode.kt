package com.arisucast.core.common.model

import java.time.Instant
import java.util.UUID

data class Episode(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val publishedAt: Instant,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val mimeType: String,
    val season: Int?,
    val episode: Int?,
    val playbackPositionMs: Long,
    val isPlayed: Boolean,
    val downloadState: DownloadState
)

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progressPercent: Int, val workerId: UUID) : DownloadState()
    data class Downloaded(val localFilePath: String, val downloadedAt: Instant) : DownloadState()
    data class Failed(val reason: String) : DownloadState()
}
