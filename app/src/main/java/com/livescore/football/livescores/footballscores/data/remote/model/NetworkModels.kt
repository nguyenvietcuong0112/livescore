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
data class MatchDetailDto(
    val fixture: FixtureDto,
    val league: LeagueDto,
    val teams: TeamsContainerDto,
    val goals: GoalsDto,
    val score: ScoreDto?,
    val statistics: List<StatisticItemDto> = emptyList(),
    val events: List<EventItemDto> = emptyList(),
    val lineups: List<LineupItemDto> = emptyList()
)

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

// --- DEVICE REGISTRATION DTOs ---

data class RegisterDeviceRequest(
    val device_id: String,
    val push_token: String?,
    val os_type: String = "android",
    val os_version: String,
    val app_version: String,
    val model_name: String,
    val language_code: String
)

data class RegisterDeviceResponse(
    val device_id: String,
    val push_token: String?,
    val device_info: DeviceInfo?,
    val subscription: SubscriptionInfo?,
    val favorites: FavoritesInfo?,
    val created_at: String?,
    val last_active_at: String?
)

data class DeviceInfo(
    val os_type: String?,
    val os_version: String?,
    val app_version: String?,
    val model_name: String?,
    val language_code: String?,
    val country_code: String?,
    val ip_address: String?
)

data class SubscriptionInfo(
    val is_vip: Boolean,
    val vip_expire_at: String?,
    val purchase_token: String?
)

data class FavoritesInfo(
    val fixtures: List<Int>?,
    val teams: List<Int>?,
    val leagues: List<Int>?
)

data class PredictionResponse(
    val code: Int,
    val message: String,
    val data: PredictionDataDto?
)

data class PredictionDataDto(
    val fixture_id: Int,
    val lang: String?,
    val home_team: PredictionTeamDto,
    val away_team: PredictionTeamDto,
    val winner_prediction: String?,
    val score_prediction: PredictionScoreDto?,
    val confidence_score: Int?,
    val over_under_prediction: String?,
    val btts_prediction: Boolean?,
    val form_overview: String?,
    val strengths_weaknesses: StrengthsWeaknessesDto?,
    val squad_impact: String?,
    val tactical_analysis: String?,
    val key_stats: List<String>?,
    val prominent_players: List<ProminentPlayerDto>?,
    val corners_prediction: TeamStatPredictionDto?,
    val yellow_cards_prediction: TeamStatPredictionDto?
)

data class PredictionTeamDto(
    val id: Int,
    val name: String,
    val logo: String,
    val winner: Boolean?
)

data class PredictionScoreDto(
    val home: Int?,
    val away: Int?
)

data class StrengthsWeaknessesDto(
    val home_strengths: List<String>?,
    val home_weaknesses: List<String>?,
    val away_strengths: List<String>?,
    val away_weaknesses: List<String>?
)

data class ProminentPlayerDto(
    val player_name: String,
    val team: String,
    val probability: Int,
    val reason: String?
)

data class TeamStatPredictionDto(
    val home: Int?,
    val away: Int?
)


