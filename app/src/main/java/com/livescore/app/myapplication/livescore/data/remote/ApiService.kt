package com.livescore.app.myapplication.livescore.data.remote

import com.livescore.app.myapplication.livescore.data.remote.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("fixtures")
    suspend fun getLiveMatches(@Query("live") live: String = "all"): FixtureResponse<MatchItemDto>

    @GET("fixtures")
    suspend fun getMatchDetail(@Query("id") matchId: Int): FixtureResponse<MatchDetailDto>

    @GET("fixtures/statistics")
    suspend fun getMatchStatistics(@Query("fixture") matchId: Int): FixtureResponse<StatisticItemDto>

    @GET("fixtures/events")
    suspend fun getMatchEvents(@Query("fixture") matchId: Int): FixtureResponse<EventItemDto>

    @GET("fixtures/lineups")
    suspend fun getMatchLineups(@Query("fixture") matchId: Int): FixtureResponse<LineupItemDto>
}
