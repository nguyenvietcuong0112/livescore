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

    fun getStandings(leagueId: Int, season: Int): Flow<List<StandingRowDto>> = flow {
        try {
            val response = apiService.getStandings(leagueId, season)
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                val wrapper = response.response.firstOrNull()
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
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                emit(response.response)
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
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                emit(response.response)
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
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                emit(response.response)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
