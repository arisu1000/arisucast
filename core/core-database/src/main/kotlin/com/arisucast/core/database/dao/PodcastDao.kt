package com.arisucast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arisucast.core.database.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title ASC")
    fun getSubscribedPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun getByFeedUrl(feedUrl: String): PodcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(podcasts: List<PodcastEntity>)

    @Update
    suspend fun update(podcast: PodcastEntity)

    @Query("UPDATE podcasts SET isSubscribed = :subscribed WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: String, subscribed: Boolean)

    @Query("DELETE FROM podcasts WHERE id = :id AND isSubscribed = 0")
    suspend fun deleteIfUnsubscribed(id: String)
}
