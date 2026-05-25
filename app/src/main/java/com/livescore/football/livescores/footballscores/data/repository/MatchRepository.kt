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
import java.util.Date
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
                fallbackToLiveMockMatches()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackToLiveMockMatches()
        }
    }

    private suspend fun fallbackToLiveMockMatches() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val mockMatches = getMockCachedMatchesForDate(todayStr).filter {
            it.statusShort in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
        }
        matchDao.clearAndInsertMatches(mockMatches)
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
                fallbackToDateMockMatches(dateStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackToDateMockMatches(dateStr)
        }
    }

    private suspend fun fallbackToDateMockMatches(dateStr: String) {
        val mockMatches = getMockCachedMatchesForDate(dateStr)
        matchDao.clearAndInsertMatchesForDate(mockMatches, dateStr)
    }

    private fun getMockCachedMatchesForDate(dateStr: String): List<CachedMatchEntity> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val isBefore = dateStr < todayStr
        val isAfter = dateStr > todayStr

        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        val min = if (currentMinute == 0) 1 else currentMinute

        // Define status/goals for Match 1 (Arsenal vs Chelsea)
        val m1Status: String
        val m1StatusLong: String
        val m1Elapsed: Int?
        val m1GoalsHome: Int?
        val m1GoalsAway: Int?
        if (isBefore) {
            m1Status = "FT"; m1StatusLong = "Match Finished"; m1Elapsed = 90; m1GoalsHome = 2; m1GoalsAway = 1
        } else if (isAfter) {
            m1Status = "NS"; m1StatusLong = "Not Started"; m1Elapsed = null; m1GoalsHome = null; m1GoalsAway = null
        } else {
            // Live today
            m1Elapsed = if (min <= 45) min else if (min in 46..50) 45 else min - 5
            m1Status = if (min <= 45) "1H" else if (min in 46..50) "HT" else "2H"
            m1StatusLong = if (min <= 45) "First Half" else if (min in 46..50) "Halftime" else "Second Half"
            m1GoalsHome = if (min > 30) 2 else 1
            m1GoalsAway = if (min > 15) 1 else 0
        }

        // Define status/goals for Match 2 (Real Madrid vs Barcelona)
        val m2Status: String
        val m2StatusLong: String
        val m2Elapsed: Int?
        val m2GoalsHome: Int?
        val m2GoalsAway: Int?
        if (isBefore) {
            m2Status = "FT"; m2StatusLong = "Match Finished"; m2Elapsed = 90; m2GoalsHome = 1; m2GoalsAway = 3
        } else if (isAfter) {
            m2Status = "NS"; m2StatusLong = "Not Started"; m2Elapsed = null; m2GoalsHome = null; m2GoalsAway = null
        } else {
            // Live today
            m2Elapsed = if (min <= 45) min else if (min in 46..50) 45 else min - 5
            m2Status = if (min <= 45) "1H" else if (min in 46..50) "HT" else "2H"
            m2StatusLong = if (min <= 45) "First Half" else if (min in 46..50) "Halftime" else "Second Half"
            m2GoalsHome = if (min > 40) 3 else 2
            m2GoalsAway = if (min > 20) 3 else 2
        }

        // Define status/goals for Match 3 (Man United vs Tottenham)
        val m3Status: String
        val m3StatusLong: String
        val m3Elapsed: Int?
        val m3GoalsHome: Int?
        val m3GoalsAway: Int?
        if (isBefore) {
            m3Status = "FT"; m3StatusLong = "Match Finished"; m3Elapsed = 90; m3GoalsHome = 3; m3GoalsAway = 2
        } else if (isAfter) {
            m3Status = "NS"; m3StatusLong = "Not Started"; m3Elapsed = null; m3GoalsHome = null; m3GoalsAway = null
        } else {
            // Finished earlier today
            m3Status = "FT"; m3StatusLong = "Match Finished"; m3Elapsed = 90; m3GoalsHome = 2; m3GoalsAway = 0
        }

        // Define status/goals for Match 4 (Liverpool vs Aston Villa)
        val m4Status: String
        val m4StatusLong: String
        val m4Elapsed: Int?
        val m4GoalsHome: Int?
        val m4GoalsAway: Int?
        if (isBefore) {
            m4Status = "FT"; m4StatusLong = "Match Finished"; m4Elapsed = 90; m4GoalsHome = 1; m4GoalsAway = 1
        } else if (isAfter) {
            m4Status = "NS"; m4StatusLong = "Not Started"; m4Elapsed = null; m4GoalsHome = null; m4GoalsAway = null
        } else {
            // Scheduled for later tonight
            m4Status = "NS"; m4StatusLong = "Not Started"; m4Elapsed = null; m4GoalsHome = null; m4GoalsAway = null
        }

        // Define status/goals for Match 5 (Bayern Munich vs Dortmund)
        val m5Status: String
        val m5StatusLong: String
        val m5Elapsed: Int?
        val m5GoalsHome: Int?
        val m5GoalsAway: Int?
        if (isBefore) {
            m5Status = "FT"; m5StatusLong = "Match Finished"; m5Elapsed = 90; m5GoalsHome = 4; m5GoalsAway = 2
        } else if (isAfter) {
            m5Status = "NS"; m5StatusLong = "Not Started"; m5Elapsed = null; m5GoalsHome = null; m5GoalsAway = null
        } else {
            // Scheduled for later tonight
            m5Status = "NS"; m5StatusLong = "Not Started"; m5Elapsed = null; m5GoalsHome = null; m5GoalsAway = null
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        fun parseTimestamp(timeStr: String): Long {
            return try {
                val date = sdf.parse("$dateStr $timeStr")
                date.time / 1000
            } catch (e: Exception) {
                System.currentTimeMillis() / 1000
            }
        }

        return listOf(
            CachedMatchEntity(
                id = 103294,
                leagueId = 39,
                leagueName = "Premier League",
                leagueLogo = "https://media.api-sports.io/football/leagues/39.png",
                homeTeamId = 42,
                homeTeamName = "Arsenal",
                homeTeamLogo = "https://media.api-sports.io/football/teams/42.png",
                awayTeamId = 49,
                awayTeamName = "Chelsea",
                awayTeamLogo = "https://media.api-sports.io/football/teams/49.png",
                statusShort = m1Status,
                elapsed = m1Elapsed,
                goalsHome = m1GoalsHome,
                goalsAway = m1GoalsAway,
                dateTimestamp = parseTimestamp("18:00"),
                statusLong = m1StatusLong,
                queryDate = dateStr
            ),
            CachedMatchEntity(
                id = 103295,
                leagueId = 140,
                leagueName = "La Liga",
                leagueLogo = "https://media.api-sports.io/football/leagues/140.png",
                homeTeamId = 541,
                homeTeamName = "Real Madrid",
                homeTeamLogo = "https://media.api-sports.io/football/teams/541.png",
                awayTeamId = 529,
                awayTeamName = "Barcelona",
                awayTeamLogo = "https://media.api-sports.io/football/teams/529.png",
                statusShort = m2Status,
                elapsed = m2Elapsed,
                goalsHome = m2GoalsHome,
                goalsAway = m2GoalsAway,
                dateTimestamp = parseTimestamp("20:00"),
                statusLong = m2StatusLong,
                queryDate = dateStr
            ),
            CachedMatchEntity(
                id = 103296,
                leagueId = 39,
                leagueName = "Premier League",
                leagueLogo = "https://media.api-sports.io/football/leagues/39.png",
                homeTeamId = 33,
                homeTeamName = "Manchester United",
                homeTeamLogo = "https://media.api-sports.io/football/teams/33.png",
                awayTeamId = 47,
                awayTeamName = "Tottenham",
                awayTeamLogo = "https://media.api-sports.io/football/teams/47.png",
                statusShort = m3Status,
                elapsed = m3Elapsed,
                goalsHome = m3GoalsHome,
                goalsAway = m3GoalsAway,
                dateTimestamp = parseTimestamp("14:00"),
                statusLong = m3StatusLong,
                queryDate = dateStr
            ),
            CachedMatchEntity(
                id = 103297,
                leagueId = 39,
                leagueName = "Premier League",
                leagueLogo = "https://media.api-sports.io/football/leagues/39.png",
                homeTeamId = 40,
                homeTeamName = "Liverpool",
                homeTeamLogo = "https://media.api-sports.io/football/teams/40.png",
                awayTeamId = 66,
                awayTeamName = "Aston Villa",
                awayTeamLogo = "https://media.api-sports.io/football/teams/66.png",
                statusShort = m4Status,
                elapsed = m4Elapsed,
                goalsHome = m4GoalsHome,
                goalsAway = m4GoalsAway,
                dateTimestamp = parseTimestamp("22:30"),
                statusLong = m4StatusLong,
                queryDate = dateStr
            ),
            CachedMatchEntity(
                id = 103298,
                leagueId = 78,
                leagueName = "Bundesliga",
                leagueLogo = "https://media.api-sports.io/football/leagues/78.png",
                homeTeamId = 157,
                homeTeamName = "Bayern Munich",
                homeTeamLogo = "https://media.api-sports.io/football/teams/157.png",
                awayTeamId = 165,
                awayTeamName = "Dortmund",
                awayTeamLogo = "https://media.api-sports.io/football/teams/165.png",
                statusShort = m5Status,
                elapsed = m5Elapsed,
                goalsHome = m5GoalsHome,
                goalsAway = m5GoalsAway,
                dateTimestamp = parseTimestamp("23:45"),
                statusLong = m5StatusLong,
                queryDate = dateStr
            )
        )
    }

    // --- Detail Api Calls with Robust Mock Fallbacks for Suspended API ---
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
                emit(getMockMatchDetail(id))
            }
        } catch (e: Exception) {
            emit(getMockMatchDetail(id))
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
                emit(getMockStatistics(id))
            }
        } catch (e: Exception) {
            emit(getMockStatistics(id))
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
                emit(getMockEvents(id))
            }
        } catch (e: Exception) {
            emit(getMockEvents(id))
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
                emit(getMockLineups(id))
            }
        } catch (e: Exception) {
            emit(getMockLineups(id))
        }
    }

    private fun getMockMatchDetail(id: Int): MatchDetailDto {
        val homeTeam: TeamDto
        val awayTeam: TeamDto
        val scoreHome: Int
        val scoreAway: Int
        val elapsed: Int
        val statusShort: String
        val statusLong: String
        val leagueName: String
        val leagueId: Int
        val leagueLogo: String

        when (id) {
            103294 -> {
                homeTeam = TeamDto(42, "Arsenal", "https://media.api-sports.io/football/teams/42.png", null)
                awayTeam = TeamDto(49, "Chelsea", "https://media.api-sports.io/football/teams/49.png", null)
                scoreHome = 2; scoreAway = 1; elapsed = 34; statusShort = "1H"; statusLong = "First Half"
                leagueName = "Premier League"; leagueId = 39; leagueLogo = "https://media.api-sports.io/football/leagues/39.png"
            }
            103295 -> {
                homeTeam = TeamDto(541, "Real Madrid", "https://media.api-sports.io/football/teams/541.png", null)
                awayTeam = TeamDto(529, "Barcelona", "https://media.api-sports.io/football/teams/529.png", null)
                scoreHome = 2; scoreAway = 2; elapsed = 72; statusShort = "2H"; statusLong = "Second Half"
                leagueName = "La Liga"; leagueId = 140; leagueLogo = "https://media.api-sports.io/football/leagues/140.png"
            }
            103296 -> {
                homeTeam = TeamDto(33, "Manchester United", "https://media.api-sports.io/football/teams/33.png", null)
                awayTeam = TeamDto(47, "Tottenham", "https://media.api-sports.io/football/teams/47.png", null)
                scoreHome = 2; scoreAway = 0; elapsed = 90; statusShort = "FT"; statusLong = "Match Finished"
                leagueName = "Premier League"; leagueId = 39; leagueLogo = "https://media.api-sports.io/football/leagues/39.png"
            }
            else -> {
                homeTeam = TeamDto(1, "Home Team", "", null)
                awayTeam = TeamDto(2, "Away Team", "", null)
                scoreHome = 1; scoreAway = 0; elapsed = 15; statusShort = "1H"; statusLong = "First Half"
                leagueName = "Local League"; leagueId = 999; leagueLogo = ""
            }
        }

        return MatchItemDto(
            fixture = FixtureDto(
                id = id,
                referee = "Michael Oliver",
                timezone = "UTC",
                date = "2026-05-22T18:00:00+00:00",
                timestamp = System.currentTimeMillis() / 1000,
                periods = PeriodsDto(null, null),
                venue = VenueDto(1, "Emirates Stadium", "London"),
                status = StatusDto(statusLong, statusShort, elapsed)
            ),
            league = LeagueDto(leagueId, leagueName, "England", leagueLogo, null, 2026, null),
            teams = TeamsContainerDto(homeTeam, awayTeam),
            goals = GoalsDto(scoreHome, scoreAway),
            score = null
        )
    }

    private fun getMockStatistics(id: Int): List<StatisticItemDto> {
        val homeTeam = TeamDto(1, "Home", "", null)
        val awayTeam = TeamDto(2, "Away", "", null)

        val homeStats = listOf(
            StatEntryDto("Ball Possession", "56%"),
            StatEntryDto("Total Shots", 14),
            StatEntryDto("Shots on Target", 6),
            StatEntryDto("Corner Kicks", 5)
        )
        val awayStats = listOf(
            StatEntryDto("Ball Possession", "44%"),
            StatEntryDto("Total Shots", 8),
            StatEntryDto("Shots on Target", 3),
            StatEntryDto("Corner Kicks", 4)
        )

        return listOf(
            StatisticItemDto(homeTeam, homeStats),
            StatisticItemDto(awayTeam, awayStats)
        )
    }

    private fun getMockEvents(id: Int): List<EventItemDto> {
        val homeTeam = TeamDto(1, "Home", "", null)
        val awayTeam = TeamDto(2, "Away", "", null)

        return listOf(
            EventItemDto(
                time = EventTimeDto(12, null),
                team = homeTeam,
                player = EventPlayerDto(101, "Bukayo Saka"),
                assist = EventPlayerDto(102, "Martin Odegaard"),
                type = "Goal",
                detail = "Normal Goal",
                comments = "Home Goal"
            ),
            EventItemDto(
                time = EventTimeDto(28, null),
                team = awayTeam,
                player = EventPlayerDto(201, "Cole Palmer"),
                assist = null,
                type = "Card",
                detail = "Yellow Card",
                comments = "Away Card"
            ),
            EventItemDto(
                time = EventTimeDto(65, null),
                team = homeTeam,
                player = EventPlayerDto(103, "Gabriel Martinelli"),
                assist = EventPlayerDto(104, "Kai Havertz"),
                type = "Subst",
                detail = "Substitution",
                comments = "Home Substitution"
            )
        )
    }

    private fun getMockLineups(id: Int): List<LineupItemDto> {
        val homeTeam = TeamDto(1, "Home", "", null)
        val awayTeam = TeamDto(2, "Away", "", null)

        val homePlayers = listOf(
            LineupPlayerWrapperDto(LineupPlayerDto(101, "D. Raya", 1, "G", "1:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(102, "B. White", 4, "D", "2:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(103, "W. Saliba", 2, "D", "2:2")),
            LineupPlayerWrapperDto(LineupPlayerDto(104, "Gabriel", 6, "D", "2:3")),
            LineupPlayerWrapperDto(LineupPlayerDto(105, "J. Timber", 12, "D", "2:4")),
            LineupPlayerWrapperDto(LineupPlayerDto(106, "M. Odegaard", 8, "M", "3:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(107, "T. Partey", 5, "M", "3:2")),
            LineupPlayerWrapperDto(LineupPlayerDto(108, "D. Rice", 41, "M", "3:3")),
            LineupPlayerWrapperDto(LineupPlayerDto(109, "B. Saka", 7, "F", "4:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(110, "K. Havertz", 29, "F", "4:2")),
            LineupPlayerWrapperDto(LineupPlayerDto(111, "G. Martinelli", 11, "F", "4:3"))
        )

        val awayPlayers = listOf(
            LineupPlayerWrapperDto(LineupPlayerDto(201, "R. Sanchez", 1, "G", "1:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(202, "M. Gusto", 27, "D", "2:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(203, "W. Fofana", 29, "D", "2:2")),
            LineupPlayerWrapperDto(LineupPlayerDto(204, "L. Colwill", 6, "D", "2:3")),
            LineupPlayerWrapperDto(LineupPlayerDto(205, "M. Cucurella", 3, "D", "2:4")),
            LineupPlayerWrapperDto(LineupPlayerDto(206, "M. Caicedo", 25, "M", "3:1")),
            LineupPlayerWrapperDto(LineupPlayerDto(207, "E. Fernandez", 8, "M", "3:2")),
            LineupPlayerWrapperDto(LineupPlayerDto(208, "N. Madueke", 11, "M", "3:3")),
            LineupPlayerWrapperDto(LineupPlayerDto(209, "C. Palmer", 20, "M", "3:4")),
            LineupPlayerWrapperDto(LineupPlayerDto(210, "J. Sancho", 19, "M", "3:5")),
            LineupPlayerWrapperDto(LineupPlayerDto(211, "N. Jackson", 15, "F", "4:1"))
        )

        return listOf(
            LineupItemDto(homeTeam, CoachDto(null, "M. Arteta", null), "4-3-3", homePlayers, emptyList()),
            LineupItemDto(awayTeam, CoachDto(null, "E. Maresca", null), "4-2-3-1", awayPlayers, emptyList())
        )
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

