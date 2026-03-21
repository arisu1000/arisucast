package com.arisucast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arisucast.core.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE podcastId = :podcastId")
    suspend fun getById(podcastId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE podcastId = :podcastId")
    suspend fun delete(podcastId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE podcastId = :podcastId)")
    suspend fun isSubscribed(podcastId: String): Boolean

    @Query("UPDATE subscriptions SET lastRefreshedAt = :timestamp WHERE podcastId = :podcastId")
    suspend fun updateLastRefreshed(podcastId: String, timestamp: Long)

    @Query("UPDATE subscriptions SET autoDownload = :autoDownload WHERE podcastId = :podcastId")
    suspend fun updateAutoDownload(podcastId: String, autoDownload: Boolean)
}
