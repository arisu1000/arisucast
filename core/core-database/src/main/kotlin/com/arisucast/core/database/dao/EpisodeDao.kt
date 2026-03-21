package com.arisucast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arisucast.core.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    fun getEpisodesByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("""
        SELECT e.* FROM episodes e
        INNER JOIN subscriptions s ON e.podcastId = s.podcastId
        ORDER BY e.publishedAt DESC
        LIMIT :limit
    """)
    fun getRecentEpisodes(limit: Int = 50): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun observeById(id: String): Flow<EpisodeEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    // Upsert that preserves playback position and played state for existing episodes
    @Query("""
        INSERT OR REPLACE INTO episodes
        (id, podcastId, title, description, audioUrl, imageUrl, publishedAt, durationSeconds,
         fileSizeBytes, mimeType, season, episode,
         playbackPositionMs, isPlayed, downloadStatus, localFilePath, downloadWorkerId, downloadedAt)
        VALUES (:id, :podcastId, :title, :description, :audioUrl, :imageUrl, :publishedAt,
                :durationSeconds, :fileSizeBytes, :mimeType, :season, :episode,
                COALESCE((SELECT playbackPositionMs FROM episodes WHERE id = :id), 0),
                COALESCE((SELECT isPlayed FROM episodes WHERE id = :id), 0),
                COALESCE((SELECT downloadStatus FROM episodes WHERE id = :id), 'NONE'),
                (SELECT localFilePath FROM episodes WHERE id = :id),
                (SELECT downloadWorkerId FROM episodes WHERE id = :id),
                (SELECT downloadedAt FROM episodes WHERE id = :id))
    """)
    suspend fun upsertPreservingProgress(
        id: String, podcastId: String, title: String, description: String,
        audioUrl: String, imageUrl: String, publishedAt: Long, durationSeconds: Int,
        fileSizeBytes: Long, mimeType: String, season: Int, episode: Int
    )

    @Query("UPDATE episodes SET playbackPositionMs = :positionMs WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, positionMs: Long)

    @Query("UPDATE episodes SET isPlayed = 1, playbackPositionMs = :durationMs WHERE id = :id")
    suspend fun markAsPlayed(id: String, durationMs: Long)

    @Query("UPDATE episodes SET downloadStatus = :status, localFilePath = :path, downloadWorkerId = :workerId, downloadedAt = :downloadedAt WHERE id = :id")
    suspend fun updateDownloadState(id: String, status: String, path: String?, workerId: String?, downloadedAt: Long?)

    @Query("SELECT * FROM episodes WHERE downloadStatus = 'DONE' ORDER BY downloadedAt DESC")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>
}
