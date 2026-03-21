package com.arisucast.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId"), Index("publishedAt")]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val publishedAt: Long,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val mimeType: String,
    val season: Int,
    val episode: Int,
    val playbackPositionMs: Long,
    val isPlayed: Boolean,
    val downloadStatus: String,
    val localFilePath: String?,
    val downloadWorkerId: String?,
    val downloadedAt: Long?
)
