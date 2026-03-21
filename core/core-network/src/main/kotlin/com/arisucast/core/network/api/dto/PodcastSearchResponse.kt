package com.arisucast.core.network.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PodcastSearchResponse(
    val status: String = "",
    val feeds: List<PodcastFeedDto> = emptyList(),
    val count: Int = 0
)

@Serializable
data class PodcastFeedDto(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    @SerialName("originalUrl") val originalUrl: String = "",
    val link: String = "",
    val description: String = "",
    val author: String = "",
    @SerialName("ownerName") val ownerName: String = "",
    val image: String = "",
    val artwork: String = "",
    val language: String = "",
    val categories: Map<String, String> = emptyMap()
)
