package com.arisucast.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val podcastId: String,
    val feedUrl: String,
    val subscribedAt: Long,
    val autoDownload: Boolean,
    val lastRefreshedAt: Long?
)
