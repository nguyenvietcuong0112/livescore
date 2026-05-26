package com.livescore.football.livescores.footballscores.data.repository

import com.livescore.football.livescores.footballscores.data.local.dao.FavoriteDao
import com.livescore.football.livescores.footballscores.data.local.dao.MatchDao
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.livescore.football.livescores.footballscores.data.local.entity.FavoriteLeagueEntity
import com.livescore.football.livescores.footballscores.data.local.entity.FavoriteTeamEntity
import com.livescore.football.livescores.footballscores.data.remote.ApiService
import com.livescore.football.livescores.footballscores.data.remote.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

    fun getCachedMatchesByQueryDate(dateStr: String): Flow<List<CachedMatchEntity>> {
        return matchDao.getCachedMatchesByQueryDate(dateStr)
    }

    suspend fun refreshLiveMatches() {
        try {
            val response = apiService.getLiveMatches()
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
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
                        statusLong = dto.fixture.status.long,
                        queryDate = todayStr
                    )
                }
                matchDao.clearAndInsertMatches(entities)
            } else {
                matchDao.clearLiveMatches()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                matchDao.clearLiveMatches()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    suspend fun refreshMatchesByDate(dateStr: String) {
        try {
            val response = apiService.getMatchesByDate(dateStr)
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
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
                        statusLong = dto.fixture.status.long,
                        queryDate = dateStr
                    )
                }
                matchDao.clearAndInsertMatchesForDate(entities, dateStr)
            } else {
                matchDao.clearMatchesByQueryDate(dateStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                matchDao.clearMatchesByQueryDate(dateStr)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // --- Detail Api Calls with Zero Mock Fallbacks ---
    fun getMatchDetail(id: Int): Flow<MatchDetailDto?> = flow {
        try {
            val response = apiService.getMatchDetail(id)
            val errors = response.errors
            val hasErrors = when {
                errors == null -> false
                errors is List<*> -> errors.isNotEmpty()
                errors is Map<*, *> -> errors.isNotEmpty()
                else -> true
            }
            if (!hasErrors && response.response.isNotEmpty()) {
                emit(response.response.firstOrNull())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(null)
        }
    }

    fun getMatchStatistics(id: Int): Flow<List<StatisticItemDto>> = flow {
        try {
            val response = apiService.getMatchStatistics(id)
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

    fun getMatchEvents(id: Int): Flow<List<EventItemDto>> = flow {
        try {
            val response = apiService.getMatchEvents(id)
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

    fun getMatchLineups(id: Int): Flow<List<LineupItemDto>> = flow {
        try {
            val response = apiService.getMatchLineups(id)
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

    // --- Favorites ---
    val favoriteTeams: Flow<List<FavoriteTeamEntity>> = favoriteDao.getAllFavoriteTeams()
    val favoriteLeagues: Flow<List<FavoriteLeagueEntity>> = favoriteDao.getAllFavoriteLeagues()

    suspend fun toggleFavoriteTeam(id: Int, name: String, logo: String) {
        val exists = favoriteDao.isTeamFavorite(id)
        if (exists > 0) {
            favoriteDao.deleteFavoriteTeam(FavoriteTeamEntity(id, name, logo))
        } else {
            favoriteDao.insertFavoriteTeam(FavoriteTeamEntity(id, name, logo))
        }
    }

    suspend fun toggleFavoriteLeague(id: Int, name: String, logo: String, country: String) {
        val exists = favoriteDao.isLeagueFavorite(id)
        if (exists > 0) {
            favoriteDao.deleteFavoriteLeague(FavoriteLeagueEntity(id, name, logo, country))
        } else {
            favoriteDao.insertFavoriteLeague(FavoriteLeagueEntity(id, name, logo, country))
        }
    }

    suspend fun getCachedMatchById(id: Int): CachedMatchEntity? {
        return matchDao.getCachedMatchById(id)
    }

    suspend fun getCachedMatchDetail(id: Int): MatchDetailDto? {
        return getCachedMatchById(id)?.toMapDetailDto()
    }
}

fun CachedMatchEntity.toMapDetailDto(): MatchItemDto {
    return MatchItemDto(
        fixture = FixtureDto(
            id = id,
            referee = null,
            timezone = "UTC",
            date = "",
            timestamp = dateTimestamp,
            periods = null,
            venue = null,
            status = StatusDto(
                long = statusLong,
                short = statusShort,
                elapsed = elapsed
            )
        ),
        league = LeagueDto(
            id = leagueId,
            name = leagueName,
            country = "",
            logo = leagueLogo,
            flag = null,
            season = 2026,
            round = null
        ),
        teams = TeamsContainerDto(
            home = TeamDto(id = homeTeamId, name = homeTeamName, logo = homeTeamLogo, winner = null),
            away = TeamDto(id = awayTeamId, name = awayTeamName, logo = awayTeamLogo, winner = null)
        ),
        goals = GoalsDto(
            home = goalsHome,
            away = goalsAway
        ),
        score = null
    )
}
