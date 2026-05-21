package com.livescore.app.myapplication.livescore.data.remote

import com.livescore.app.myapplication.livescore.data.remote.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("api/live")
    suspend fun getLiveMatches(): FixtureResponse<MatchItemDto>

    @GET("api/match/{id}")
    suspend fun getMatchDetail(@Path("id") matchId: Int): FixtureResponse<MatchDetailDto>

    @GET("api/match/{id}/stats")
    suspend fun getMatchStatistics(@Path("id") matchId: Int): FixtureResponse<StatisticItemDto>

    @GET("api/match/{id}/events")
    suspend fun getMatchEvents(@Path("id") matchId: Int): FixtureResponse<EventItemDto>

    @GET("api/match/{id}/lineups")
    suspend fun getMatchLineups(@Path("id") matchId: Int): FixtureResponse<LineupItemDto>
}
