package com.livescore.football.livescores.footballscores.data.repository

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

    fun getLeagues(): Flow<List<ServerLeagueDto>> = flow {
        try {
            val response = apiService.getLeagues()
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getStandings(leagueId: Int, season: Int): Flow<List<StandingRowDto>> = flow {
        try {
            val response = apiService.getStandings(leagueId, season)
            if (response.code == 200 && response.data.isNotEmpty()) {
                val wrapper = response.data.firstOrNull()
                val standingsList = wrapper?.league?.standings?.flatten() ?: emptyList()
                emit(standingsList)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getTopScorers(leagueId: Int, season: Int): Flow<List<TopPlayerItemDto>> = flow {
        try {
            val response = apiService.getTopScorers(leagueId, season)
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getTopAssists(leagueId: Int, season: Int): Flow<List<TopPlayerItemDto>> = flow {
        try {
            val response = apiService.getTopAssists(leagueId, season)
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    fun getFixturesByLeague(leagueId: Int, season: Int): Flow<List<MatchItemDto>> = flow {
        try {
            val response = apiService.getFixturesByLeague(leagueId, season)
            if (response.code == 200 && response.data.isNotEmpty()) {
                emit(response.data)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
