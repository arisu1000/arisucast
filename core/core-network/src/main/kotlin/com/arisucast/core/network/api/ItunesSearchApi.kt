package com.arisucast.core.network.api

import com.arisucast.core.network.api.dto.ItunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesSearchApi {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") query: String,
        @Query("media") media: String = "podcast",
        @Query("limit") limit: Int = 20,
        @Query("entity") entity: String = "podcast"
    ): ItunesSearchResponse
}
