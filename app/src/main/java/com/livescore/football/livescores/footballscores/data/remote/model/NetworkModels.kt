package com.livescore.football.livescores.footballscores.data.remote.model

// --- COMMON DTOs ---

data class BaseResponse<T>(
    val code: Int,
    val message: String,
    val data: List<T>
)

data class ServerLeagueDto(
    val _id: String,
    val league_id: Int,
    val name: String,
    val type: String,
    val logo: String,
    val country: ServerCountryDto?,
    val current_season: ServerSeasonDto?,
    val is_popular: Boolean,
    val priority: Int,
    val is_active: Boolean,
    val sync_priority: String,
    val live_detail_ttl_seconds: Int,
    val created_at: String,
    val updated_at: String
)

data class ServerCountryDto(
    val name: String?,
    val code: String?,
    val flag: String?
)

data class ServerSeasonDto(
    val year: Int,
    val start: String?,
    val end: String?
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
