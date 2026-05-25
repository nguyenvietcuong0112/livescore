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

    private val _leagues = MutableStateFlow(
        listOf(
            LeagueSelectorItem(1, "World Cup", "https://media.api-sports.io/football/leagues/1.png", "World"),
            LeagueSelectorItem(39, "Premier League", "https://media.api-sports.io/football/leagues/39.png", "England"),
            LeagueSelectorItem(140, "La Liga", "https://media.api-sports.io/football/leagues/140.png", "Spain"),
            LeagueSelectorItem(135, "Serie A", "https://media.api-sports.io/football/leagues/135.png", "Italy"),
            LeagueSelectorItem(78, "Bundesliga", "https://media.api-sports.io/football/leagues/78.png", "Germany"),
            LeagueSelectorItem(61, "Ligue 1", "https://media.api-sports.io/football/leagues/61.png", "France"),
            LeagueSelectorItem(2, "Champions League", "https://media.api-sports.io/football/leagues/2.png", "Europe")
        )
    )
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
        // Automatically fetch data whenever selectedLeagueId or selectedSeason changes
        viewModelScope.launch {
            combine(_selectedLeagueId, _selectedSeason) { leagueId, season ->
                Pair(leagueId, season)
            }.collect { (leagueId, season) ->
                fetchAllLeagueData(leagueId, season)
            }
        }
    }

    fun selectLeague(leagueId: Int) {
        _selectedLeagueId.value = leagueId
        if (leagueId == 1) {
            _selectedSeason.value = 2026
        } else {
            _selectedSeason.value = calculateCurrentSeason()
        }
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
                // Sequential, spaced-out fetching to protect free tier rate limit bounds
                repository.getStandings(leagueId, season)
                    .catch { e ->
                        _standingsState.value = StandingsUiState.Error(e.message ?: "Failed to load standings")
                    }
                    .collect { list ->
                        _standingsState.value = StandingsUiState.Success(list)
                    }

                delay(2000)

                repository.getTopScorers(leagueId, season)
                    .catch { e ->
                        _topScorersState.value = TopPlayersUiState.Error(e.message ?: "Failed to load top scorers")
                    }
                    .collect { list ->
                        _topScorersState.value = TopPlayersUiState.Success(list)
                    }

                delay(2000)

                repository.getTopAssists(leagueId, season)
                    .catch { e ->
                        _topAssistsState.value = TopPlayersUiState.Error(e.message ?: "Failed to load top assists")
                    }
                    .collect { list ->
                        _topAssistsState.value = TopPlayersUiState.Success(list)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
