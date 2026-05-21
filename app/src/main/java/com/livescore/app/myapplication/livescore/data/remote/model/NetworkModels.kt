package com.livescore.app.myapplication.livescore.data.remote.model

// --- COMMON DTOs ---

data class FixtureResponse<T>(
    val get: String,
    val parameters: Map<String, String>?,
    val errors: Any?,
    val results: Int,
    val paging: PagingDto?,
    val response: List<T>
)

data class PagingDto(
    val current: Int,
    val total: Int
)

data class FixtureDto(
    val id: Int,
    val referee: String?,
    val timezone: String,
    val date: String,
    val timestamp: Long,
    val periods: PeriodsDto?,
    val venue: VenueDto?,
    val status: StatusDto
)

data class PeriodsDto(
    val first: Long?,
    val second: Long?
)

data class VenueDto(
    val id: Int?,
    val name: String?,
    val city: String?
)

data class StatusDto(
    val long: String,
    val short: String,
    val elapsed: Int?
)

data class LeagueDto(
    val id: Int,
    val name: String,
    val country: String,
    val logo: String,
    val flag: String?,
    val season: Int,
    val round: String?
)

data class TeamDto(
    val id: Int,
    val name: String,
    val logo: String,
    val winner: Boolean?
)

data class TeamsContainerDto(
    val home: TeamDto,
    val away: TeamDto
)

data class GoalsDto(
    val home: Int?,
    val away: Int?
)

data class ScoreDetailDto(
    val home: Int?,
    val away: Int?
)

data class ScoreDto(
    val halftime: ScoreDetailDto?,
    val fulltime: ScoreDetailDto?,
    val extratime: ScoreDetailDto?,
    val penalty: ScoreDetailDto?
)

// --- INDIVIDUAL API RESPONSE ITEM TYPES ---

// 1. Live Match Item (represented by full Fixture object)
data class MatchItemDto(
    val fixture: FixtureDto,
    val league: LeagueDto,
    val teams: TeamsContainerDto,
    val goals: GoalsDto,
    val score: ScoreDto?
)

// 2. Match Detail (contains events, lineups, statistics too if fetched in details, or just the same structure)
typealias MatchDetailDto = MatchItemDto

// 3. Match Statistics DTO
data class StatisticItemDto(
    val team: TeamDto,
    val statistics: List<StatEntryDto>
)

data class StatEntryDto(
    val type: String,
    val value: Any? // Can be string (like "56%") or Int
)

// 4. Match Events DTO
data class EventItemDto(
    val time: EventTimeDto,
    val team: TeamDto,
    val player: EventPlayerDto,
    val assist: EventPlayerDto?,
    val type: String, // Goal, Card, Subst, Var
    val detail: String, // Normal Goal, Yellow Card, etc
    val comments: String?
)

data class EventTimeDto(
    val elapsed: Int,
    val extra: Int?
)

data class EventPlayerDto(
    val id: Int?,
    val name: String?
)

// 5. Match Lineups DTO
data class LineupItemDto(
    val team: TeamDto,
    val coach: CoachDto?,
    val formation: String?,
    val startXI: List<LineupPlayerWrapperDto>,
    val substitutes: List<LineupPlayerWrapperDto>
)

data class CoachDto(
    val id: Int?,
    val name: String?,
    val photo: String?
)

data class LineupPlayerWrapperDto(
    val player: LineupPlayerDto
)

data class LineupPlayerDto(
    val id: Int,
    val name: String,
    val number: Int,
    val pos: String?, // G, D, M, F
    val grid: String? // e.g. "1:1", "3:4:1:2"
)
