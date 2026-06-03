package com.livescore.football.livescores.footballscores.data.repository

import android.util.Log
import com.livescore.football.livescores.footballscores.data.remote.ApiService
import com.livescore.football.livescores.footballscores.data.remote.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaguesRepository @Inject constructor(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "LeaguesRepository"
    }

    fun getLeagues(): Flow<List<ServerLeagueDto>> = flow {
        Log.d(TAG, "getLeagues: Starting request")
        try {
            val response = apiService.getLeagues()
            Log.d(TAG, "getLeagues: Response code = ${response.code}, data size = ${response.data?.size ?: 0}")
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                Log.w(TAG, "getLeagues: Response is empty or code != 200")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "getLeagues: Error occurred: ${e.message}", e)
            emit(emptyList())
        }
    }

    fun getStandings(leagueId: Int, season: Int): Flow<List<StandingRowDto>> = flow {
        Log.d(TAG, "getStandings: Starting request for leagueId = $leagueId, season = $season")
        try {
            val response = apiService.getStandings(leagueId, season)
            Log.d(TAG, "getStandings: Response code = ${response.code}, data size = ${response.data?.size ?: 0}")
            if (response.code == 200 && response.data.isNotEmpty()) {
                val wrapper = response.data.firstOrNull()
                val standingsList = wrapper?.league?.standings?.flatten() ?: emptyList()
                Log.d(TAG, "getStandings: Extracted standings list size = ${standingsList.size}")
                emit(standingsList)
            } else {
                Log.w(TAG, "getStandings: Response is empty or code != 200")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "getStandings: Error occurred: ${e.message}", e)
            emit(emptyList())
        }
    }

    fun getTopScorers(leagueId: Int, season: Int): Flow<List<TopPlayerItemDto>> = flow {
        Log.d(TAG, "getTopScorers: Starting request for leagueId = $leagueId, season = $season")
        try {
            val response = apiService.getTopScorers(leagueId, season)
            Log.d(TAG, "getTopScorers: Response code = ${response.code}, data size = ${response.data?.size ?: 0}")
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                Log.w(TAG, "getTopScorers: Response is empty or code != 200")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTopScorers: Error occurred: ${e.message}", e)
            emit(emptyList())
        }
    }

    fun getTopAssists(leagueId: Int, season: Int): Flow<List<TopPlayerItemDto>> = flow {
        Log.d(TAG, "getTopAssists: Starting request for leagueId = $leagueId, season = $season")
        try {
            val response = apiService.getTopAssists(leagueId, season)
            Log.d(TAG, "getTopAssists: Response code = ${response.code}, data size = ${response.data?.size ?: 0}")
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                Log.w(TAG, "getTopAssists: Response is empty or code != 200")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTopAssists: Error occurred: ${e.message}", e)
            emit(emptyList())
        }
    }

    fun getFixturesByLeague(leagueId: Int, season: Int): Flow<List<MatchItemDto>> = flow {
        Log.d(TAG, "getFixturesByLeague: Starting request for leagueId = $leagueId, season = $season")
        try {
            val response = apiService.getFixturesByLeague(leagueId, season)
            Log.d(TAG, "getFixturesByLeague: Response code = ${response.code}, data size = ${response.data?.size ?: 0}")
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                Log.w(TAG, "getFixturesByLeague: Response is empty or code != 200")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFixturesByLeague: Error occurred: ${e.message}", e)
            emit(emptyList())
        }
    }
}
