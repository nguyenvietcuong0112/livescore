package com.livescore.app.myapplication.livescore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.data.repository.MatchRepository
import com.livescore.app.myapplication.livescore.data.local.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class MatchFilter {
    LIVE, UPCOMING, FINISHED
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val favoriteManager: FavoriteManager
) : ViewModel() {

    private val _selectedDate = MutableStateFlow<Date>(Calendar.getInstance().time)
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _currentFilter = MutableStateFlow(MatchFilter.LIVE)
    val currentFilter: StateFlow<MatchFilter> = _currentFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favoriteFixtureIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteFixtureIds: StateFlow<Set<String>> = _favoriteFixtureIds.asStateFlow()

    private var pollingJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val matches: StateFlow<List<MatchListItem>> = _selectedDate.flatMapLatest { date ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(date)
        repository.getCachedMatchesByQueryDate(dateStr)
    }.combine(_currentFilter) { filteredMatches, filter ->
        when (filter) {
            MatchFilter.LIVE -> filteredMatches.filter { isLiveStatus(it.statusShort) }
            MatchFilter.UPCOMING -> filteredMatches.filter { it.statusShort == "NS" || it.statusShort == "TBD" }
            MatchFilter.FINISHED -> filteredMatches.filter { it.statusShort == "FT" || it.statusShort == "AET" || it.statusShort == "PEN" }
        }
    }.combine(_favoriteFixtureIds) { filteredMatches, favIds ->
        val items = mutableListOf<MatchListItem>()
        val grouped = filteredMatches.groupBy { it.leagueId }
        var matchCount = 0

        for ((leagueId, matchGroup) in grouped) {
            val firstMatch = matchGroup.first()
            items.add(MatchListItem.LeagueHeader(leagueId, firstMatch.leagueName, firstMatch.leagueLogo))
            for (match in matchGroup) {
                val isFav = favIds.contains(match.id.toString())
                items.add(MatchListItem.MatchItem(match, isFav))
                matchCount++
                if (matchCount % 5 == 0) {
                    items.add(MatchListItem.NativeAd(
                        id = "ad_$matchCount",
                        title = "Unlock SofaScore Premium",
                        body = "Ad-free experience with real-time pressure pitch maps!"
                    ))
                }
            }
        }
        items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshFavorites()
        setSelectedDate(Calendar.getInstance().time)
    }

    fun refreshFavorites() {
        _favoriteFixtureIds.value = favoriteManager.getFavoriteFixtureIds()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        if (isToday(date)) {
            _currentFilter.value = MatchFilter.LIVE
            startLivePolling()
            refreshMatchesForDate(date)
        } else if (isBeforeToday(date)) {
            _currentFilter.value = MatchFilter.FINISHED
            stopLivePolling()
            refreshMatchesForDate(date)
        } else {
            _currentFilter.value = MatchFilter.UPCOMING
            stopLivePolling()
            refreshMatchesForDate(date)
        }
    }

    fun setFilter(filter: MatchFilter) {
        _currentFilter.value = filter
        val date = _selectedDate.value
        if (filter == MatchFilter.LIVE && isToday(date)) {
            startLivePolling()
        } else {
            stopLivePolling()
            refreshMatchesForDate(date)
        }
    }

    fun startLivePolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                _isLoading.value = true
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                repository.refreshMatchesByDate(todayStr)
                _isLoading.value = false
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun stopLivePolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshMatchesForDate(date: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(date)
            repository.refreshMatchesByDate(dateStr)
            _isLoading.value = false
        }
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
     }

    private fun isBeforeToday(date: Date): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.before(today)
    }

    private fun isLiveStatus(status: String): Boolean {
        return status in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
    }

    fun toggleFavorite(match: CachedMatchEntity) {
        viewModelScope.launch {
            favoriteManager.toggleFixtureFavorite(match.id)
            refreshFavorites()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLivePolling()
    }
}
