package com.livescore.app.myapplication.livescore.data.repository

import com.livescore.app.myapplication.livescore.data.remote.ApiService
import com.livescore.app.myapplication.livescore.data.remote.model.*
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
                val standingsList = wrapper?.league?.standings?.firstOrNull() ?: emptyList()
                if (standingsList.isNotEmpty()) {
                    emit(standingsList)
                } else {
                    emit(generateMockStandings(leagueId))
                }
            } else {
                emit(generateMockStandings(leagueId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(generateMockStandings(leagueId))
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
                emit(generateMockTopPlayers(leagueId, isAssists = false))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(generateMockTopPlayers(leagueId, isAssists = false))
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
                emit(generateMockTopPlayers(leagueId, isAssists = true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(generateMockTopPlayers(leagueId, isAssists = true))
        }
    }

    // --- Mock Data Generators ---

    private fun generateMockStandings(leagueId: Int): List<StandingRowDto> {
        val teams = when (leagueId) {
            1 -> listOf( // World Cup (1)
                Triple("Argentina 🇦🇷", "https://media.api-sports.io/football/teams/26.png", 26),
                Triple("France 🇫🇷", "https://media.api-sports.io/football/teams/2.png", 2),
                Triple("Brazil 🇧🇷", "https://media.api-sports.io/football/teams/6.png", 6),
                Triple("England 🏴󠁧󠁢󠁥󠁮󠁧󠁿", "https://media.api-sports.io/football/teams/10.png", 10),
                Triple("Portugal 🇵🇹", "https://media.api-sports.io/football/teams/27.png", 27),
                Triple("Spain 🇪🇸", "https://media.api-sports.io/football/teams/9.png", 9),
                Triple("Germany 🇩🇪", "https://media.api-sports.io/football/teams/25.png", 25),
                Triple("Netherlands 🇳🇱", "https://media.api-sports.io/football/teams/1118.png", 1118),
                Triple("Italy 🇮🇹", "https://media.api-sports.io/football/teams/768.png", 768),
                Triple("Japan 🇯🇵", "https://media.api-sports.io/football/teams/12.png", 12),
                Triple("South Korea 🇰🇷", "https://media.api-sports.io/football/teams/771.png", 771),
                Triple("Vietnam 🇻🇳", "https://media.api-sports.io/football/teams/1500.png", 1500)
            )
            2 -> listOf( // Champions League (2)
                Triple("Real Madrid", "https://media.api-sports.io/football/teams/541.png", 541),
                Triple("Manchester City", "https://media.api-sports.io/football/teams/50.png", 50),
                Triple("Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 157),
                Triple("Arsenal", "https://media.api-sports.io/football/teams/42.png", 42),
                Triple("Barcelona", "https://media.api-sports.io/football/teams/529.png", 529),
                Triple("Inter", "https://media.api-sports.io/football/teams/505.png", 505),
                Triple("Atletico Madrid", "https://media.api-sports.io/football/teams/530.png", 530),
                Triple("Dortmund", "https://media.api-sports.io/football/teams/165.png", 165),
                Triple("Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 85),
                Triple("Liverpool", "https://media.api-sports.io/football/teams/40.png", 40),
                Triple("Juventus", "https://media.api-sports.io/football/teams/496.png", 496),
                Triple("Milan", "https://media.api-sports.io/football/teams/489.png", 489)
            )
            61 -> listOf( // Ligue 1 (61)
                Triple("Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 85),
                Triple("Marseille", "https://media.api-sports.io/football/teams/81.png", 81),
                Triple("Monaco", "https://media.api-sports.io/football/teams/91.png", 91),
                Triple("Lens", "https://media.api-sports.io/football/teams/116.png", 116),
                Triple("Lille", "https://media.api-sports.io/football/teams/79.png", 79),
                Triple("Rennes", "https://media.api-sports.io/football/teams/94.png", 94),
                Triple("Lyon", "https://media.api-sports.io/football/teams/80.png", 80),
                Triple("Nice", "https://media.api-sports.io/football/teams/84.png", 84),
                Triple("Reims", "https://media.api-sports.io/football/teams/93.png", 93),
                Triple("Montpellier", "https://media.api-sports.io/football/teams/82.png", 82),
                Triple("Toulouse", "https://media.api-sports.io/football/teams/96.png", 96),
                Triple("Nantes", "https://media.api-sports.io/football/teams/83.png", 83)
            )
            140 -> listOf( // La Liga
                Triple("Real Madrid", "https://media.api-sports.io/football/teams/541.png", 541),
                Triple("Barcelona", "https://media.api-sports.io/football/teams/529.png", 529),
                Triple("Atletico Madrid", "https://media.api-sports.io/football/teams/530.png", 530),
                Triple("Real Sociedad", "https://media.api-sports.io/football/teams/548.png", 548),
                Triple("Villarreal", "https://media.api-sports.io/football/teams/533.png", 533),
                Triple("Real Betis", "https://media.api-sports.io/football/teams/543.png", 543),
                Triple("Athletic Bilbao", "https://media.api-sports.io/football/teams/531.png", 531),
                Triple("Girona", "https://media.api-sports.io/football/teams/547.png", 547),
                Triple("Sevilla", "https://media.api-sports.io/football/teams/536.png", 536),
                Triple("Osasuna", "https://media.api-sports.io/football/teams/527.png", 527),
                Triple("Valencia", "https://media.api-sports.io/football/teams/532.png", 532),
                Triple("Celta Vigo", "https://media.api-sports.io/football/teams/538.png", 538),
                Triple("Mallorca", "https://media.api-sports.io/football/teams/539.png", 539),
                Triple("Rayo Vallecano", "https://media.api-sports.io/football/teams/546.png", 546),
                Triple("Getafe", "https://media.api-sports.io/football/teams/545.png", 545),
                Triple("Cadiz", "https://media.api-sports.io/football/teams/542.png", 542),
                Triple("Almeria", "https://media.api-sports.io/football/teams/544.png", 544),
                Triple("Valladolid", "https://media.api-sports.io/football/teams/528.png", 528),
                Triple("Espanyol", "https://media.api-sports.io/football/teams/540.png", 540),
                Triple("Elche", "https://media.api-sports.io/football/teams/537.png", 537)
            )
            135 -> listOf( // Serie A
                Triple("Napoli", "https://media.api-sports.io/football/teams/492.png", 492),
                Triple("Lazio", "https://media.api-sports.io/football/teams/487.png", 487),
                Triple("Inter", "https://media.api-sports.io/football/teams/505.png", 505),
                Triple("Milan", "https://media.api-sports.io/football/teams/489.png", 489),
                Triple("Atalanta", "https://media.api-sports.io/football/teams/499.png", 499),
                Triple("Roma", "https://media.api-sports.io/football/teams/497.png", 497),
                Triple("Juventus", "https://media.api-sports.io/football/teams/496.png", 496),
                Triple("Fiorentina", "https://media.api-sports.io/football/teams/502.png", 502),
                Triple("Bologna", "https://media.api-sports.io/football/teams/500.png", 500),
                Triple("Torino", "https://media.api-sports.io/football/teams/503.png", 503),
                Triple("Monza", "https://media.api-sports.io/football/teams/1579.png", 1579),
                Triple("Udinese", "https://media.api-sports.io/football/teams/494.png", 494),
                Triple("Sassuolo", "https://media.api-sports.io/football/teams/488.png", 488),
                Triple("Empoli", "https://media.api-sports.io/football/teams/511.png", 511),
                Triple("Salernitana", "https://media.api-sports.io/football/teams/512.png", 512),
                Triple("Lecce", "https://media.api-sports.io/football/teams/517.png", 517),
                Triple("Spezia", "https://media.api-sports.io/football/teams/515.png", 515),
                Triple("Verona", "https://media.api-sports.io/football/teams/504.png", 504),
                Triple("Cremonese", "https://media.api-sports.io/football/teams/6425.png", 6425),
                Triple("Sampdoria", "https://media.api-sports.io/football/teams/498.png", 498)
            )
            78 -> listOf( // Bundesliga
                Triple("Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 157),
                Triple("Dortmund", "https://media.api-sports.io/football/teams/165.png", 165),
                Triple("RB Leipzig", "https://media.api-sports.io/football/teams/173.png", 173),
                Triple("Union Berlin", "https://media.api-sports.io/football/teams/182.png", 182),
                Triple("Freiburg", "https://media.api-sports.io/football/teams/160.png", 160),
                Triple("Bayer Leverkusen", "https://media.api-sports.io/football/teams/168.png", 168),
                Triple("Eintracht Frankfurt", "https://media.api-sports.io/football/teams/169.png", 169),
                Triple("Wolfsburg", "https://media.api-sports.io/football/teams/161.png", 161),
                Triple("Mainz 05", "https://media.api-sports.io/football/teams/164.png", 164),
                Triple("Monchengladbach", "https://media.api-sports.io/football/teams/163.png", 163),
                Triple("FC Koln", "https://media.api-sports.io/football/teams/167.png", 167),
                Triple("Hoffenheim", "https://media.api-sports.io/football/teams/162.png", 162),
                Triple("Werder Bremen", "https://media.api-sports.io/football/teams/162.png", 162), // werder
                Triple("VfL Bochum", "https://media.api-sports.io/football/teams/176.png", 176),
                Triple("Augsburg", "https://media.api-sports.io/football/teams/170.png", 170),
                Triple("VfB Stuttgart", "https://media.api-sports.io/football/teams/172.png", 172),
                Triple("Schalke 04", "https://media.api-sports.io/football/teams/174.png", 174),
                Triple("Hertha Berlin", "https://media.api-sports.io/football/teams/159.png", 159)
            )
            else -> listOf( // Premier League (39) default
                Triple("Arsenal", "https://media.api-sports.io/football/teams/42.png", 42),
                Triple("Manchester City", "https://media.api-sports.io/football/teams/50.png", 50),
                Triple("Newcastle United", "https://media.api-sports.io/football/teams/34.png", 34),
                Triple("Manchester United", "https://media.api-sports.io/football/teams/33.png", 33),
                Triple("Liverpool", "https://media.api-sports.io/football/teams/40.png", 40),
                Triple("Brighton", "https://media.api-sports.io/football/teams/51.png", 51),
                Triple("Aston Villa", "https://media.api-sports.io/football/teams/66.png", 66),
                Triple("Tottenham Hotspur", "https://media.api-sports.io/football/teams/47.png", 47),
                Triple("Brentford", "https://media.api-sports.io/football/teams/55.png", 55),
                Triple("Fulham", "https://media.api-sports.io/football/teams/36.png", 36),
                Triple("Crystal Palace", "https://media.api-sports.io/football/teams/52.png", 52),
                Triple("Chelsea", "https://media.api-sports.io/football/teams/49.png", 49),
                Triple("Wolves", "https://media.api-sports.io/football/teams/39.png", 39),
                Triple("West Ham United", "https://media.api-sports.io/football/teams/48.png", 48),
                Triple("Bournemouth", "https://media.api-sports.io/football/teams/35.png", 35),
                Triple("Nottingham Forest", "https://media.api-sports.io/football/teams/65.png", 65),
                Triple("Everton", "https://media.api-sports.io/football/teams/45.png", 45),
                Triple("Leicester City", "https://media.api-sports.io/football/teams/46.png", 46),
                Triple("Leeds United", "https://media.api-sports.io/football/teams/63.png", 63),
                Triple("Southampton", "https://media.api-sports.io/football/teams/41.png", 41)
            )
        }

        // Generate games played (P = 38, 34 for Bundesliga/Ligue1, 8 for UCL, 3 for World Cup group)
        val maxGames = when (leagueId) {
            78 -> 34
            61 -> 34
            2 -> 8
            1 -> 3
            else -> 38
        }
        
        return teams.mapIndexed { index, triple ->
            val rank = index + 1
            val win = (maxGames - rank - (index / 3)).coerceAtLeast(index % 2 + 1).coerceAtMost(maxGames)
            val lose = (rank + index / 4).coerceAtMost(maxGames - win).coerceAtLeast(0)
            val draw = (maxGames - win - lose).coerceAtLeast(0)
            val pts = win * 3 + draw
            val gf = (90 - index * 3).coerceAtLeast(3).coerceAtLeast(maxGames)
            val ga = (25 + index * 2).coerceAtLeast(1).coerceAtLeast(maxGames)
            val gd = gf - ga

            val description = when (leagueId) {
                1 -> when (rank) {
                    1, 2 -> "Promotion - Round of 16"
                    else -> null
                }
                2 -> when (rank) {
                    in 1..4 -> "Promotion - Round of 16"
                    in 5..8 -> "Promotion - Play-offs"
                    else -> null
                }
                61 -> when (rank) {
                    1, 2, 3 -> "Promotion - Champions League (Group Stage)"
                    4 -> "Promotion - Champions League (Qualifications)"
                    5 -> "Promotion - Europa League"
                    in teams.size - 2 .. teams.size -> "Relegation"
                    else -> null
                }
                else -> when (rank) {
                    1, 2, 3, 4 -> "Promotion - Champions League (Group Stage)"
                    5 -> if (leagueId == 78) "Promotion - Europa League (Group Stage)" else "Promotion - Europa League (Group Stage)"
                    6 -> if (leagueId == 78) "Promotion - Conference League (Play Offs)" else "Promotion - Europa League (Group Stage)"
                    7 -> if (leagueId != 78) "Promotion - Conference League (Play Offs)" else null
                    in teams.size - 2 .. teams.size -> "Relegation"
                    else -> null
                }
            }

            val form = listOf("W", "D", "W", "L", "W").shuffled().take(maxGames.coerceAtMost(5)).joinToString("")

            StandingRowDto(
                rank = rank,
                team = StandingTeamDto(id = triple.third, name = triple.first, logo = triple.second),
                points = pts,
                goalsDiff = gd,
                group = if (leagueId == 1) "Group Stage" else "League Table",
                form = form,
                status = "same",
                description = description,
                all = StandingStatsDto(
                    played = maxGames,
                    win = win,
                    draw = draw,
                    lose = lose,
                    goals = StandingGoalsDto(goalsFor = gf, against = ga)
                ),
                home = null,
                away = null
            )
        }
    }

    private fun generateMockTopPlayers(leagueId: Int, isAssists: Boolean): List<TopPlayerItemDto> {
        val list = when (leagueId) {
            1 -> { // World Cup (1)
                if (isAssists) listOf(
                    Quad("Lionel Messi", "Argentina 🇦🇷", "https://media.api-sports.io/football/teams/26.png", 5),
                    Quad("Kylian Mbappe", "France 🇫🇷", "https://media.api-sports.io/football/teams/2.png", 4),
                    Quad("Bruno Fernandes", "Portugal 🇵🇹", "https://media.api-sports.io/football/teams/27.png", 4),
                    Quad("Antoine Griezmann", "France 🇫🇷", "https://media.api-sports.io/football/teams/2.png", 3),
                    Quad("Harry Kane", "England 🏴󠁧󠁢󠁥󠁮󠁧󠁿", "https://media.api-sports.io/football/teams/10.png", 3),
                    Quad("Nguyen Quang Hai 🇻🇳", "Vietnam 🇻🇳", "https://media.api-sports.io/football/teams/1500.png", 2)
                ) else listOf(
                    Quad("Kylian Mbappe", "France 🇫🇷", "https://media.api-sports.io/football/teams/2.png", 8),
                    Quad("Lionel Messi", "Argentina 🇦🇷", "https://media.api-sports.io/football/teams/26.png", 7),
                    Quad("Julian Alvarez", "Argentina 🇦🇷", "https://media.api-sports.io/football/teams/26.png", 4),
                    Quad("Olivier Giroud", "France 🇫🇷", "https://media.api-sports.io/football/teams/2.png", 4),
                    Quad("Alvaro Morata", "Spain 🇪🇸", "https://media.api-sports.io/football/teams/9.png", 3),
                    Quad("Nguyen Tien Linh 🇻🇳", "Vietnam 🇻🇳", "https://media.api-sports.io/football/teams/1500.png", 3)
                )
            }
            2 -> { // Champions League (2)
                if (isAssists) listOf(
                    Quad("Vinicius Junior", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 6),
                    Quad("Kevin De Bruyne", "Manchester City", "https://media.api-sports.io/football/teams/50.png", 5),
                    Quad("Federico Dimarco", "Inter", "https://media.api-sports.io/football/teams/505.png", 5),
                    Quad("João Mário", "Benfica", "https://media.api-sports.io/football/teams/190.png", 4),
                    Quad("Lionel Messi", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 4),
                    Quad("Kylian Mbappe", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 3)
                ) else listOf(
                    Quad("Erling Haaland", "Manchester City", "https://media.api-sports.io/football/teams/50.png", 12),
                    Quad("Mohamed Salah", "Liverpool", "https://media.api-sports.io/football/teams/40.png", 8),
                    Quad("Kylian Mbappe", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 7),
                    Quad("Vinicius Junior", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 7),
                    Quad("João Mário", "Benfica", "https://media.api-sports.io/football/teams/190.png", 6),
                    Quad("Olivier Giroud", "Milan", "https://media.api-sports.io/football/teams/489.png", 5)
                )
            }
            61 -> { // Ligue 1 (61)
                if (isAssists) listOf(
                    Quad("Lionel Messi", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 16),
                    Quad("Neymar Jr", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 11),
                    Quad("Jonathan Clauss", "Marseille", "https://media.api-sports.io/football/teams/81.png", 9),
                    Quad("Remy Cabella", "Lille", "https://media.api-sports.io/football/teams/79.png", 9),
                    Quad("Kylian Mbappe", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 6),
                    Quad("Caio Henrique", "Monaco", "https://media.api-sports.io/football/teams/91.png", 9)
                ) else listOf(
                    Quad("Kylian Mbappe", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 29),
                    Quad("Alexandre Lacazette", "Lyon", "https://media.api-sports.io/football/teams/80.png", 27),
                    Quad("Jonathan David", "Lille", "https://media.api-sports.io/football/teams/79.png", 24),
                    Quad("Folarin Balogun", "Reims", "https://media.api-sports.io/football/teams/93.png", 21),
                    Quad("Lois Openda", "Lens", "https://media.api-sports.io/football/teams/116.png", 21),
                    Quad("Neymar Jr", "Paris Saint Germain", "https://media.api-sports.io/football/teams/85.png", 13)
                )
            }
            140 -> { // La Liga
                if (isAssists) listOf(
                    Quad("Antoine Griezmann", "Atletico Madrid", "https://media.api-sports.io/football/teams/530.png", 16),
                    Quad("Vinicius Junior", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 13),
                    Quad("Rodrygo Goes", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 10),
                    Quad("Robert Lewandowski", "Barcelona", "https://media.api-sports.io/football/teams/529.png", 8),
                    Quad("Pedri Gonzalez", "Barcelona", "https://media.api-sports.io/football/teams/529.png", 7),
                    Quad("Federico Valverde", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 7)
                ) else listOf(
                    Quad("Robert Lewandowski", "Barcelona", "https://media.api-sports.io/football/teams/529.png", 23),
                    Quad("Antoine Griezmann", "Atletico Madrid", "https://media.api-sports.io/football/teams/530.png", 15),
                    Quad("Karim Benzema", "Real Madrid", "https://media.api-sports.io/football/teams/541.png", 19),
                    Quad("Joselu Mato", "Espanyol", "https://media.api-sports.io/football/teams/540.png", 16),
                    Quad("Borja Iglesias", "Real Betis", "https://media.api-sports.io/football/teams/543.png", 15),
                    Quad("Vedat Muriqi", "Mallorca", "https://media.api-sports.io/football/teams/539.png", 15)
                )
            }
            135 -> { // Serie A
                if (isAssists) listOf(
                    Quad("Khvicha Kvaratskhelia", "Napoli", "https://media.api-sports.io/football/teams/492.png", 13),
                    Quad("Rafael Leao", "Milan", "https://media.api-sports.io/football/teams/489.png", 10),
                    Quad("Piotr Zielinski", "Napoli", "https://media.api-sports.io/football/teams/492.png", 9),
                    Quad("Sergej Milinkovic-Savic", "Lazio", "https://media.api-sports.io/football/teams/487.png", 8),
                    Quad("Lautaro Martinez", "Inter", "https://media.api-sports.io/football/teams/505.png", 7),
                    Quad("Paulo Dybala", "Roma", "https://media.api-sports.io/football/teams/497.png", 7)
                ) else listOf(
                    Quad("Victor Osimhen", "Napoli", "https://media.api-sports.io/football/teams/492.png", 26),
                    Quad("Lautaro Martinez", "Inter", "https://media.api-sports.io/football/teams/505.png", 21),
                    Quad("Boulaye Dia", "Salernitana", "https://media.api-sports.io/football/teams/512.png", 16),
                    Quad("Rafael Leao", "Milan", "https://media.api-sports.io/football/teams/489.png", 15),
                    Quad("Olivier Giroud", "Milan", "https://media.api-sports.io/football/teams/489.png", 13),
                    Quad("Ademola Lookman", "Atalanta", "https://media.api-sports.io/football/teams/499.png", 13)
                )
            }
            78 -> { // Bundesliga
                if (isAssists) listOf(
                    Quad("Raphael Guerreiro", "Dortmund", "https://media.api-sports.io/football/teams/165.png", 12),
                    Quad("Randal Kolo Muani", "Frankfurt", "https://media.api-sports.io/football/teams/169.png", 11),
                    Quad("Jamal Musiala", "Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 10),
                    Quad("Florian Kainz", "FC Koln", "https://media.api-sports.io/football/teams/167.png", 10),
                    Quad("Jonas Hofmann", "Gladbach", "https://media.api-sports.io/football/teams/163.png", 9),
                    Quad("Thomas Muller", "Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 8)
                ) else listOf(
                    Quad("Niclas Fullkrug", "Werder Bremen", "https://media.api-sports.io/football/teams/162.png", 16),
                    Quad("Christopher Nkunku", "RB Leipzig", "https://media.api-sports.io/football/teams/173.png", 16),
                    Quad("Randal Kolo Muani", "Frankfurt", "https://media.api-sports.io/football/teams/169.png", 15),
                    Quad("Vincenzo Grifo", "Freiburg", "https://media.api-sports.io/football/teams/160.png", 15),
                    Quad("Serge Gnabry", "Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 14),
                    Quad("Jamal Musiala", "Bayern Munich", "https://media.api-sports.io/football/teams/157.png", 12)
                )
            }
            else -> { // Premier League (39)
                if (isAssists) listOf(
                    Quad("Kevin De Bruyne", "Manchester City", "https://media.api-sports.io/football/teams/50.png", 16),
                    Quad("Mohamed Salah", "Liverpool", "https://media.api-sports.io/football/teams/40.png", 12),
                    Quad("Leandro Trossard", "Arsenal", "https://media.api-sports.io/football/teams/42.png", 12),
                    Quad("Bukayo Saka", "Arsenal", "https://media.api-sports.io/football/teams/42.png", 11),
                    Quad("Michael Olise", "Crystal Palace", "https://media.api-sports.io/football/teams/52.png", 11),
                    Quad("Riyad Mahrez", "Manchester City", "https://media.api-sports.io/football/teams/50.png", 10)
                ) else listOf(
                    Quad("Erling Haaland", "Manchester City", "https://media.api-sports.io/football/teams/50.png", 36),
                    Quad("Harry Kane", "Tottenham Hotspur", "https://media.api-sports.io/football/teams/47.png", 30),
                    Quad("Ivan Toney", "Brentford", "https://media.api-sports.io/football/teams/55.png", 20),
                    Quad("Mohamed Salah", "Liverpool", "https://media.api-sports.io/football/teams/40.png", 19),
                    Quad("Callum Wilson", "Newcastle United", "https://media.api-sports.io/football/teams/34.png", 18),
                    Quad("Marcus Rashford", "Manchester United", "https://media.api-sports.io/football/teams/33.png", 17)
                )
            }
        }

        return list.map { quad ->
            TopPlayerItemDto(
                player = TopPlayerPlayerDto(
                    id = quad.playerName.hashCode(),
                    name = quad.playerName,
                    firstname = quad.playerName.split(" ").firstOrNull(),
                    lastname = quad.playerName.split(" ").lastOrNull(),
                    age = 26,
                    nationality = "Various",
                    height = "180 cm",
                    weight = "75 kg",
                    photo = "https://media.api-sports.io/football/players/${(quad.playerName.hashCode() % 10000).coerceAtLeast(100)}.png"
                ),
                statistics = listOf(
                    TopPlayerStatDto(
                        team = StandingTeamDto(id = quad.teamName.hashCode(), name = quad.teamName, logo = quad.teamLogo),
                        league = null,
                        games = TopPlayerGamesDto(
                            appearances = 34,
                            lineups = 32,
                            minutes = 2800,
                            number = null,
                            position = "Attacker",
                            rating = "7.8",
                            captain = false
                        ),
                        goals = TopPlayerGoalsDto(
                            total = if (isAssists) 5 else quad.value,
                            assists = if (isAssists) quad.value else 5,
                            saves = null,
                            concessions = null
                        ),
                        penalty = null
                    )
                )
            )
        }
    }

    private data class Quad(
        val playerName: String,
        val teamName: String,
        val teamLogo: String,
        val value: Int
    )
}
