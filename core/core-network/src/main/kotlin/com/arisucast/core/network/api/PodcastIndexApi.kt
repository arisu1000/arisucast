package com.arisucast.core.network.api

import com.arisucast.core.network.api.dto.PodcastSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApi {
    @GET("search/byterm")
    suspend fun searchByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 20,
        @Query("clean") clean: Boolean = false
    ): PodcastSearchResponse
}
