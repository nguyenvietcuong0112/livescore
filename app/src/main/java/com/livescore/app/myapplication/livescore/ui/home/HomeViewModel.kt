package com.livescore.app.myapplication.livescore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MatchFilter {
    LIVE, UPCOMING, FINISHED
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(MatchFilter.LIVE)
    val currentFilter: StateFlow<MatchFilter> = _currentFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: Job? = null

    // Combined Flow: filter matches depending on filter type
    val matches: StateFlow<List<CachedMatchEntity>> = combine(
        repository.allCachedMatches,
        _currentFilter
    ) { allMatches, filter ->
        when (filter) {
            MatchFilter.LIVE -> allMatches.filter { isLiveStatus(it.statusShort) }
            MatchFilter.UPCOMING -> allMatches.filter { it.statusShort == "NS" || it.statusShort == "TBD" }
            MatchFilter.FINISHED -> allMatches.filter { it.statusShort == "FT" || it.statusShort == "AET" || it.statusShort == "PEN" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startLivePolling()
    }

    fun setFilter(filter: MatchFilter) {
        _currentFilter.value = filter
        if (filter == MatchFilter.LIVE) {
            startLivePolling()
        } else {
            stopLivePolling()
            // Single refresh for static historical/scheduled data
            refreshMatchesOnce()
        }
    }

    fun startLivePolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                _isLoading.value = true
                repository.refreshLiveMatches()
                _isLoading.value = false
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun stopLivePolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun refreshMatchesOnce() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshLiveMatches()
            _isLoading.value = false
        }
    }

    private fun isLiveStatus(status: String): Boolean {
        return status in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
    }

    fun toggleFavorite(match: CachedMatchEntity) {
        viewModelScope.launch {
            repository.toggleFavoriteTeam(match.homeTeamId, match.homeTeamName, match.homeTeamLogo)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLivePolling()
    }
}
