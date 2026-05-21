package com.livescore.app.myapplication.livescore.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.app.myapplication.livescore.data.remote.model.*
import com.livescore.app.myapplication.livescore.data.repository.MatchRepository
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

    // Simulation flows for canvas views
    private val _ballPosition = MutableStateFlow(Pair(0.5f, 0.5f))
    val ballPosition: StateFlow<Pair<Float, Float>> = _ballPosition.asStateFlow()

    private val _attackState = MutableStateFlow(0) // 0 = none, 1 = Home, 2 = Away
    val attackState: StateFlow<Int> = _attackState.asStateFlow()

    private val _momentumData = MutableStateFlow<List<Float>>(emptyList())
    val momentumData: StateFlow<List<Float>> = _momentumData.asStateFlow()

    fun startDetailPolling(matchId: Int) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Concurrent fetching using combine / zip
                val detailFlow = repository.getMatchDetail(matchId)
                val statsFlow = repository.getMatchStatistics(matchId)
                val eventsFlow = repository.getMatchEvents(matchId)
                val lineupsFlow = repository.getMatchLineups(matchId)

                combine(detailFlow, statsFlow, eventsFlow, lineupsFlow) { detail, stats, events, lineups ->
                    MatchDetailUiState(
                        detail = detail,
                        stats = stats,
                        events = events,
                        lineups = lineups,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                    simulateCanvasEvents()
                }

                delay(8000) // Poll every 8 seconds as per requirement
            }
        }
    }

    fun stopDetailPolling() {
        pollingJob?.cancel()
        pollingJob = null
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
