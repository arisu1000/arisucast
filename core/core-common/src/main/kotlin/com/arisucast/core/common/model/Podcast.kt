package com.arisucast.core.common.model

import java.time.Instant

data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val feedUrl: String,
    val imageUrl: String,
    val websiteUrl: String,
    val category: String,
    val language: String,
    val lastUpdated: Instant,
    val isSubscribed: Boolean,
    val isFavorite: Boolean
)
