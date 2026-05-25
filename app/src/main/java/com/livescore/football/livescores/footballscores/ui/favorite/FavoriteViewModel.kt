package com.livescore.football.livescores.footballscores.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.football.livescores.footballscores.data.local.FavoriteManager
import com.livescore.football.livescores.footballscores.data.local.MatchReminderManager
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.livescore.football.livescores.footballscores.data.repository.MatchRepository
import com.livescore.football.livescores.footballscores.ui.home.MatchListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteManager: FavoriteManager,
    private val matchRepository: MatchRepository,
    private val reminderManager: MatchReminderManager
) : ViewModel() {

    private val _favoriteMatches = MutableStateFlow<List<MatchListItem>>(emptyList())
    val favoriteMatches: StateFlow<List<MatchListItem>> = _favoriteMatches.asStateFlow()

    init {
        loadFavoriteMatches()
    }

    fun loadFavoriteMatches() {
        val favIds = favoriteManager.getFavoriteFixtureIds()
        val favIntIds = favIds.mapNotNull { it.toIntOrNull() }.toSet()
        val remindIds = reminderManager.getAllReminderIds()
        
        viewModelScope.launch {
            matchRepository.allCachedMatches.map { matches ->
                // Filter matches that are in the user's favorite set
                val filtered = matches.filter { it.id in favIntIds }
                
                // Group matches by league and map to MatchListItem
                val items = mutableListOf<MatchListItem>()
                val grouped = filtered.groupBy { it.leagueId }
                for ((leagueId, matchGroup) in grouped) {
                    val firstMatch = matchGroup.first()
                    items.add(MatchListItem.LeagueHeader(leagueId, firstMatch.leagueName, firstMatch.leagueLogo))
                    for (match in matchGroup) {
                        val isRemind = remindIds.contains(match.id)
                        items.add(MatchListItem.MatchItem(match, isFavorite = true, isReminderSet = isRemind))
                    }
                }
                items
            }.collect { items ->
                _favoriteMatches.value = items
            }
        }
    }

    fun toggleFixtureFavorite(match: CachedMatchEntity) {
        viewModelScope.launch {
            favoriteManager.toggleFixtureFavorite(match.id)
            loadFavoriteMatches()
        }
    }
}
