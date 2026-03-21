package com.arisucast.core.network.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesPodcastDto> = emptyList()
)

@Serializable
data class ItunesPodcastDto(
    @SerialName("trackId") val trackId: Long = 0,
    @SerialName("trackName") val trackName: String = "",
    @SerialName("artistName") val artistName: String = "",
    @SerialName("artworkUrl600") val artworkUrl: String = "",
    @SerialName("feedUrl") val feedUrl: String = "",
    @SerialName("primaryGenreName") val genre: String = "",
    @SerialName("trackCount") val episodeCount: Int = 0
)
