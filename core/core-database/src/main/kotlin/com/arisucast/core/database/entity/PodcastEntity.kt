package com.arisucast.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val feedUrl: String,
    val imageUrl: String,
    val websiteUrl: String,
    val category: String,
    val language: String,
    val lastUpdated: Long,
    val isSubscribed: Boolean
)
