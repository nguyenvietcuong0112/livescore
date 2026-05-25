package com.livescore.football.livescores.footballscores.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.football.livescores.footballscores.data.remote.model.*
import com.livescore.football.livescores.footballscores.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class MatchDetailUiState(
    val detail: MatchDetailDto? = null,
    val stats: List<StatisticItemDto> = emptyList(),
    val events: List<EventItemDto> = emptyList(),
    val lineups: List<LineupItemDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchDetailUiState())
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var simulationJob: Job? = null

    // Simulation flows for canvas views
    private val _ballPosition = MutableStateFlow(Pair(0.5f, 0.5f))
    val ballPosition: StateFlow<Pair<Float, Float>> = _ballPosition.asStateFlow()

    private val _attackState = MutableStateFlow(0) // 0 = none, 1 = Home, 2 = Away
    val attackState: StateFlow<Int> = _attackState.asStateFlow()

    private val _momentumData = MutableStateFlow<List<Float>>(emptyList())
    val momentumData: StateFlow<List<Float>> = _momentumData.asStateFlow()

    fun startDetailPolling(matchId: Int) {
        if (pollingJob?.isActive == true) return
        
        // Start local fluid radar animations and updates every 3 seconds
        startLocalVisualSimulation()

        pollingJob = viewModelScope.launch {
            // Load from database instantly to populate the UI (teams, logos, score, status, league)
            try {
                repository.getCachedMatchDetail(matchId)?.let { cachedDetail ->
                    _uiState.value = _uiState.value.copy(
                        detail = cachedDetail
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var isFirstLoad = true

            while (true) {
                // Only show loading spinner on the very first load if we don't even have cached detail
                if (_uiState.value.detail == null) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                try {
                    // 1. Get Match Detail FIRST and update UI instantly to show teams, logos, and scores
                    repository.getMatchDetail(matchId).collect { detail ->
                        _uiState.value = _uiState.value.copy(
                            detail = detail,
                            isLoading = false // Disable loading spinner immediately
                        )
                    }

                    // 2. Statistics
                    if (!isFirstLoad) delay(2000)
                    repository.getMatchStatistics(matchId).collect { stats ->
                        _uiState.value = _uiState.value.copy(
                            stats = stats
                        )
                    }

                    // 3. Events
                    if (!isFirstLoad) delay(2000)
                    repository.getMatchEvents(matchId).collect { events ->
                        _uiState.value = _uiState.value.copy(
                            events = events
                        )
                    }

                    // 4. Lineups
                    if (!isFirstLoad) delay(2000)
                    repository.getMatchLineups(matchId).collect { lineups ->
                        _uiState.value = _uiState.value.copy(
                            lineups = lineups
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }

                isFirstLoad = false
                delay(60000) // Poll actual network API every 60 seconds to protect Free API quota
            }
        }
    }

    private fun startLocalVisualSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                simulateCanvasEvents()
                delay(3000) // Simulate pitch ball movement every 3 seconds locally for top-tier visual flow
            }
        }
    }

    fun stopDetailPolling() {
        pollingJob?.cancel()
        pollingJob = null
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun simulateCanvasEvents() {
        // Randomly simulate ball coordinates and pressure to showcase premium canvas views
        val x = Random.nextFloat()
        val y = Random.nextFloat()
        _ballPosition.value = Pair(x, y)

        val side = Random.nextInt(3) // 0, 1 or 2
        _attackState.value = side

        // Randomize momentum pressure values
        val mockData = List(30) { Random.nextFloat() * 200 - 100 }
        _momentumData.value = mockData
    }

    override fun onCleared() {
        super.onCleared()
        stopDetailPolling()
    }
}
