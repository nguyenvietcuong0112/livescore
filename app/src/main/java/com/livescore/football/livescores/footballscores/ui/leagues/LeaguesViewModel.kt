package com.livescore.football.livescores.footballscores.ui.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.football.livescores.footballscores.data.remote.model.StandingRowDto
import com.livescore.football.livescores.footballscores.data.remote.model.TopPlayerItemDto
import com.livescore.football.livescores.footballscores.data.repository.LeaguesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StandingsUiState {
    object Loading : StandingsUiState()
    data class Success(val list: List<StandingRowDto>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

sealed class TopPlayersUiState {
    object Loading : TopPlayersUiState()
    data class Success(val list: List<TopPlayerItemDto>) : TopPlayersUiState()
    data class Error(val message: String) : TopPlayersUiState()
}

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val repository: LeaguesRepository
) : ViewModel() {

    private val _leagues = MutableStateFlow<List<LeagueSelectorItem>>(emptyList())
    val leagues: StateFlow<List<LeagueSelectorItem>> = _leagues.asStateFlow()

    private val _selectedLeagueId = MutableStateFlow(39) // Default: Premier League (39)
    val selectedLeagueId: StateFlow<Int> = _selectedLeagueId.asStateFlow()

    private val _selectedSeason = MutableStateFlow(calculateCurrentSeason())
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Standings, 1: Top Scorers, 2: Top Assists
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _standingsState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val standingsState: StateFlow<StandingsUiState> = _standingsState.asStateFlow()

    private val _topScorersState = MutableStateFlow<TopPlayersUiState>(TopPlayersUiState.Loading)
    val topScorersState: StateFlow<TopPlayersUiState> = _topScorersState.asStateFlow()

    private val _topAssistsState = MutableStateFlow<TopPlayersUiState>(TopPlayersUiState.Loading)
    val topAssistsState: StateFlow<TopPlayersUiState> = _topAssistsState.asStateFlow()

    private fun calculateCurrentSeason(): Int {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) // 0-indexed: 0 is Jan, 7 is Aug
        return if (currentMonth < java.util.Calendar.AUGUST) {
            currentYear - 1
        } else {
            currentYear
        }
    }

    init {
        fetchLeagues()
        // Fetch initial league data on startup
        fetchAllLeagueData(_selectedLeagueId.value, _selectedSeason.value)
    }

    private fun fetchLeagues() {
        viewModelScope.launch {
            repository.getLeagues()
                .catch { e -> e.printStackTrace() }
                .collect { list ->
                    val items = list.map { league ->
                        LeagueSelectorItem(
                            id = league.league_id,
                            name = league.name,
                            logo = league.logo,
                            country = league.country?.name ?: "World"
                        )
                    }
                    _leagues.value = items
                    // Select first if not present
                    if (items.isNotEmpty() && items.none { it.id == _selectedLeagueId.value }) {
                        selectLeague(items.first().id)
                    }
                }
        }
    }

    fun selectLeague(leagueId: Int) {
        _selectedLeagueId.value = leagueId
        val season = if (leagueId == 1) 2026 else calculateCurrentSeason()
        _selectedSeason.value = season
        fetchAllLeagueData(leagueId, season)
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun refreshCurrentData() {
        fetchAllLeagueData(_selectedLeagueId.value, _selectedSeason.value)
    }

    private fun fetchAllLeagueData(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _standingsState.value = StandingsUiState.Loading
            _topScorersState.value = TopPlayersUiState.Loading
            _topAssistsState.value = TopPlayersUiState.Loading

            try {
                var standings = emptyList<StandingRowDto>()
                repository.getStandings(leagueId, season)
                    .catch { e ->
                        e.printStackTrace()
                    }
                    .collect { list ->
                        standings = list
                    }
                _standingsState.value = StandingsUiState.Success(standings)

                delay(2000)

                var topScorers = emptyList<TopPlayerItemDto>()
                repository.getTopScorers(leagueId, season)
                    .catch { e ->
                        e.printStackTrace()
                    }
                    .collect { list ->
                        topScorers = list
                    }
                _topScorersState.value = TopPlayersUiState.Success(topScorers)

                delay(2000)

                var topAssists = emptyList<TopPlayerItemDto>()
                repository.getTopAssists(leagueId, season)
                    .catch { e ->
                        e.printStackTrace()
                    }
                    .collect { list ->
                        topAssists = list
                    }
                _topAssistsState.value = TopPlayersUiState.Success(topAssists)

            } catch (e: Exception) {
                e.printStackTrace()
                _standingsState.value = StandingsUiState.Error(e.message ?: "Failed to load standings")
                _topScorersState.value = TopPlayersUiState.Error(e.message ?: "Failed to load top players")
                _topAssistsState.value = TopPlayersUiState.Error(e.message ?: "Failed to load top players")
            }
        }
    }
}
