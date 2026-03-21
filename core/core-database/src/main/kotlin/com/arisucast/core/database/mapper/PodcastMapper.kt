package com.arisucast.core.database.mapper

import com.arisucast.core.common.model.Podcast
import com.arisucast.core.database.entity.PodcastEntity
import java.time.Instant

fun PodcastEntity.toDomainModel(): Podcast = Podcast(
    id = id,
    title = title,
    author = author,
    description = description,
    feedUrl = feedUrl,
    imageUrl = imageUrl,
    websiteUrl = websiteUrl,
    category = category,
    language = language,
    lastUpdated = Instant.ofEpochMilli(lastUpdated),
    isSubscribed = isSubscribed
)

fun Podcast.toEntity(): PodcastEntity = PodcastEntity(
    id = id,
    title = title,
    author = author,
    description = description,
    feedUrl = feedUrl,
    imageUrl = imageUrl,
    websiteUrl = websiteUrl,
    category = category,
    language = language,
    lastUpdated = lastUpdated.toEpochMilli(),
    isSubscribed = isSubscribed
)
