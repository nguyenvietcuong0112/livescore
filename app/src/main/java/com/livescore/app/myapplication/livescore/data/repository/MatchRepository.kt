package com.livescore.app.myapplication.livescore.data.repository

import com.livescore.app.myapplication.livescore.data.local.dao.FavoriteDao
import com.livescore.app.myapplication.livescore.data.local.dao.MatchDao
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteLeagueEntity
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteTeamEntity
import com.livescore.app.myapplication.livescore.data.remote.ApiService
import com.livescore.app.myapplication.livescore.data.remote.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val apiService: ApiService,
    private val matchDao: MatchDao,
    private val favoriteDao: FavoriteDao
) {
    // --- Cached Matches Flow ---
    val allCachedMatches: Flow<List<CachedMatchEntity>> = matchDao.getAllCachedMatches()
    val liveCachedMatches: Flow<List<CachedMatchEntity>> = matchDao.getLiveCachedMatches()

    suspend fun refreshLiveMatches() {
        try {
            val response = apiService.getLiveMatches()
            if (response.errors.isNullOrEmpty()) {
                val entities = response.response.map { dto ->
                    CachedMatchEntity(
                        id = dto.fixture.id,
                        leagueId = dto.league.id,
                        leagueName = dto.league.name,
                        leagueLogo = dto.league.logo,
                        homeTeamId = dto.teams.home.id,
                        homeTeamName = dto.teams.home.name,
                        homeTeamLogo = dto.teams.home.logo,
                        awayTeamId = dto.teams.away.id,
                        awayTeamName = dto.teams.away.name,
                        awayTeamLogo = dto.teams.away.logo,
                        statusShort = dto.fixture.status.short,
                        elapsed = dto.fixture.status.elapsed,
                        goalsHome = dto.goals.home,
                        goalsAway = dto.goals.away,
                        dateTimestamp = dto.fixture.timestamp,
                        statusLong = dto.fixture.status.long
                    )
                }
                matchDao.clearAllMatches()
                matchDao.insertMatches(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Detail Api Calls ---
    fun getMatchDetail(id: Int): Flow<MatchDetailDto?> = flow {
        try {
            val response = apiService.getMatchDetail(id)
            emit(response.response.firstOrNull())
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getMatchStatistics(id: Int): Flow<List<StatisticItemDto>> = flow {
        try {
            val response = apiService.getMatchStatistics(id)
            emit(response.response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getMatchEvents(id: Int): Flow<List<EventItemDto>> = flow {
        try {
            val response = apiService.getMatchEvents(id)
            emit(response.response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getMatchLineups(id: Int): Flow<List<LineupItemDto>> = flow {
        try {
            val response = apiService.getMatchLineups(id)
            emit(response.response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // --- Favorites ---
    val favoriteTeams: Flow<List<FavoriteTeamEntity>> = favoriteDao.getAllFavoriteTeams()
    val favoriteLeagues: Flow<List<FavoriteLeagueEntity>> = favoriteDao.getAllFavoriteLeagues()

    suspend fun toggleFavoriteTeam(id: Int, name: String, logo: String) {
        val exists = favoriteDao.isTeamFavorite(id)
        if (exists) {
            favoriteDao.deleteFavoriteTeam(FavoriteTeamEntity(id, name, logo))
        } else {
            favoriteDao.insertFavoriteTeam(FavoriteTeamEntity(id, name, logo))
        }
    }

    suspend fun toggleFavoriteLeague(id: Int, name: String, logo: String, country: String) {
        val exists = favoriteDao.isLeagueFavorite(id)
        if (exists) {
            favoriteDao.deleteFavoriteLeague(FavoriteLeagueEntity(id, name, logo, country))
        } else {
            favoriteDao.insertFavoriteLeague(FavoriteLeagueEntity(id, name, logo, country))
        }
    }
}
