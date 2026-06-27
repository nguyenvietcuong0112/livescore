package com.livescore.football.livescores.footballscores.data.remote

import com.livescore.football.livescores.footballscores.data.remote.model.*
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/v1/leagues")
    suspend fun getLeagues(): BaseResponse<ServerLeagueDto>

    @GET("api/v1/fixtures/live")
    suspend fun getLiveMatches(): BaseResponse<MatchItemDto>

    @GET("api/v1/fixtures/date")
    suspend fun getMatchesByDate(@Query("date") date: String): BaseResponse<MatchItemDto>

    @GET("api/v1/fixtures/all-date")
    suspend fun getAllMatchesByDate(@Query("date") date: String): BaseResponse<MatchItemDto>

    @GET("api/v1/fixtures/{fixture_id}/details")
    suspend fun getMatchDetail(@Path("fixture_id") matchId: Int): BaseResponse<MatchDetailDto>

    @GET("api/v1/leagues/{league_id}/standings")
    suspend fun getStandings(
        @Path("league_id") leagueId: Int,
        @Query("season") season: Int
    ): BaseResponse<StandingsLeagueWrapperDto>

    @GET("api/v1/leagues/{league_id}/topscorers")
    suspend fun getTopScorers(
        @Path("league_id") leagueId: Int,
        @Query("season") season: Int
    ): BaseResponse<TopPlayerItemDto>

    @GET("api/v1/leagues/{league_id}/topassists")
    suspend fun getTopAssists(
        @Path("league_id") leagueId: Int,
        @Query("season") season: Int
    ): BaseResponse<TopPlayerItemDto>

    @GET("api/v1/fixtures")
    suspend fun getFixturesByLeague(
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Query("round") round: String? = null
    ): BaseResponse<MatchItemDto>

    @GET("api/v1/fixtures/rounds")
    suspend fun getFixturesRounds(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): BaseResponse<String>

    @GET("api/v1/fixtures/bracket")
    suspend fun getWcBracket(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): BaseResponse<MatchItemDto>

    @GET("api/v1/fixtures/{fixture_id}/ai-prediction")
    suspend fun getAiPrediction(
        @Path("fixture_id") matchId: Int,
        @Query("lang") lang: String
    ): PredictionResponse

    @POST("api/v1/users/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): BaseResponse<RegisterDeviceResponse>

    @POST("api/v1/users/push/click/{push_id}")
    suspend fun trackPushClick(
        @Path("push_id") pushId: String
    ): BaseResponse<Any?>
}
