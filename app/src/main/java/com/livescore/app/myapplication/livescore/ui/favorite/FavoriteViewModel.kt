package com.livescore.app.myapplication.livescore.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.app.myapplication.livescore.data.local.FavoriteManager
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.data.repository.MatchRepository
import com.livescore.app.myapplication.livescore.ui.home.MatchListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteManager: FavoriteManager,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _favoriteMatches = MutableStateFlow<List<MatchListItem>>(emptyList())
    val favoriteMatches: StateFlow<List<MatchListItem>> = _favoriteMatches.asStateFlow()

    init {
        loadFavoriteMatches()
    }

    fun loadFavoriteMatches() {
        val favIds = favoriteManager.getFavoriteFixtureIds()
        val favIntIds = favIds.mapNotNull { it.toIntOrNull() }.toSet()
        
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
                        items.add(MatchListItem.MatchItem(match, isFavorite = true))
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
