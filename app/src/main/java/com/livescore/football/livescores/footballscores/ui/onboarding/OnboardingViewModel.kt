package com.livescore.football.livescores.footballscores.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livescore.football.livescores.footballscores.data.local.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingItem(
    val id: Int,
    val name: String,
    val subtitle: String,
    val logo: String,
    val type: String, // "language", "league", "team", "player"
    val isHot: Boolean = false,
    val parentId: Int = 0, // e.g. team maps to leagueId, player maps to teamId
    val flagEmoji: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteManager: FavoriteManager
) : ViewModel() {

    private val onboardingPrefs: SharedPreferences =
        context.getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)

    // Current screen: 1 = Select Leagues, 2 = Select Teams, 3 = Select Players
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selections
    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedLeagues = MutableStateFlow<Set<Int>>(emptySet())
    val selectedLeagues: StateFlow<Set<Int>> = _selectedLeagues.asStateFlow()

    private val _selectedTeams = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTeams: StateFlow<Set<Int>> = _selectedTeams.asStateFlow()

    private val _selectedPlayers = MutableStateFlow<Set<Int>>(emptySet())
    val selectedPlayers: StateFlow<Set<Int>> = _selectedPlayers.asStateFlow()

    // Event flow to notify Activity to redirect to MainActivity when completed
    private val _onboardingCompleted = MutableSharedFlow<Boolean>()
    val onboardingCompleted: SharedFlow<Boolean> = _onboardingCompleted.asSharedFlow()

    // Master Static lists
    private val masterLanguages = listOf(
        "Arabic", "English", "French", "German", "Hindi",
        "Indonesian", "Italian", "Japanese", "Portuguese", "Russian",
        "Spanish", "Thai", "Turkish", "Urdu", "Vietnamese"
    ).mapIndexed { idx, name ->
        OnboardingItem(idx, name, "", "", "language")
    }

    private val masterLeagues = listOf(
        OnboardingItem(39, "Premier League", "England", "https://media.api-sports.io/football/leagues/39.png", "league", isHot = true),
        OnboardingItem(140, "La Liga", "Spain", "https://media.api-sports.io/football/leagues/140.png", "league", isHot = true),
        OnboardingItem(2, "UEFA Champions League", "Europe", "https://media.api-sports.io/football/leagues/2.png", "league", isHot = true),
        OnboardingItem(135, "Serie A", "Italy", "https://media.api-sports.io/football/leagues/135.png", "league", isHot = false),
        OnboardingItem(78, "Bundesliga", "Germany", "https://media.api-sports.io/football/leagues/78.png", "league", isHot = false),
        OnboardingItem(61, "Ligue 1", "France", "https://media.api-sports.io/football/leagues/61.png", "league", isHot = false),
        OnboardingItem(1, "World Cup 2026", "World", "https://media.api-sports.io/football/leagues/1.png", "league", isHot = false)
    )

    private val masterTeams = listOf(
        OnboardingItem(42, "Arsenal", "Premier League", "https://media.api-sports.io/football/teams/42.png", "team", parentId = 39),
        OnboardingItem(49, "Chelsea", "Premier League", "https://media.api-sports.io/football/teams/49.png", "team", parentId = 39),
        OnboardingItem(541, "Real Madrid", "La Liga", "https://media.api-sports.io/football/teams/541.png", "team", parentId = 140),
        OnboardingItem(529, "Barcelona", "La Liga", "https://media.api-sports.io/football/teams/529.png", "team", parentId = 140),
        OnboardingItem(33, "Manchester United", "Premier League", "https://media.api-sports.io/football/teams/33.png", "team", parentId = 39),
        OnboardingItem(47, "Tottenham", "Premier League", "https://media.api-sports.io/football/teams/47.png", "team", parentId = 39),
        OnboardingItem(40, "Liverpool", "Premier League", "https://media.api-sports.io/football/teams/40.png", "team", parentId = 39),
        OnboardingItem(66, "Aston Villa", "Premier League", "https://media.api-sports.io/football/teams/66.png", "team", parentId = 39),
        OnboardingItem(157, "Bayern Munich", "Bundesliga", "https://media.api-sports.io/football/teams/157.png", "team", parentId = 78),
        OnboardingItem(165, "Dortmund", "Bundesliga", "https://media.api-sports.io/football/teams/165.png", "team", parentId = 78),
        OnboardingItem(1111, "Vietnam 🇻🇳", "World Cup 2026", "https://media.api-sports.io/football/teams/1111.png", "team", parentId = 1),
        OnboardingItem(85, "Paris Saint Germain", "Ligue 1", "https://media.api-sports.io/football/teams/85.png", "team", parentId = 61),
        OnboardingItem(50, "Man City", "Premier League", "https://media.api-sports.io/football/teams/50.png", "team", parentId = 39)
    )

    private val masterPlayers = listOf(
        OnboardingItem(10, "L. Messi", "Cầu thủ quốc tế", "", "player", parentId = 529, flagEmoji = "🇦🇷"),
        OnboardingItem(7, "C. Ronaldo", "Cầu thủ quốc tế", "", "player", parentId = 541, flagEmoji = "🇵🇹"),
        OnboardingItem(9, "K. Mbappé", "Cầu thủ quốc tế", "", "player", parentId = 541, flagEmoji = "🇫🇷"),
        OnboardingItem(19, "Quang Hải", "Cầu thủ quốc tế", "", "player", parentId = 1111, flagEmoji = "🇻🇳"),
        OnboardingItem(17, "K. De Bruyne", "Cầu thủ quốc tế", "", "player", parentId = 50, flagEmoji = "🇧🇪"),
        OnboardingItem(103, "E. Haaland", "Cầu thủ quốc tế", "", "player", parentId = 50, flagEmoji = "🇳🇴"),
        OnboardingItem(11, "M. Salah", "Cầu thủ quốc tế", "", "player", parentId = 40, flagEmoji = "🇪🇬"),
        OnboardingItem(77, "B. Saka", "Cầu thủ quốc tế", "", "player", parentId = 42, flagEmoji = "🏴\u00AD󠁢\u00AD󠁥\u00AD󠁮\u00AD󠁧\u00AD󠁿"),
        OnboardingItem(101, "J. Bellingham", "Cầu thủ quốc tế", "", "player", parentId = 541, flagEmoji = "🏴\u00AD󠁢\u00AD󠁥\u00AD󠁮\u00AD󠁧\u00AD󠁿"),
        OnboardingItem(102, "L. Yamal", "Cầu thủ quốc tế", "", "player", parentId = 529, flagEmoji = "🇪🇸")
    )

    // Highly optimized combining logic that produces real-time filtered and prioritized lists
    val uiItems: StateFlow<List<OnboardingItem>> = combine(
        _currentStep,
        _searchQuery,
        _selectedLeagues,
        _selectedTeams
    ) { step, query, selLeagues, selTeams ->
        when (step) {
            0 -> {
                // Language screen: Filter alphabetically or by text match
                if (query.isEmpty()) masterLanguages
                else masterLanguages.filter { it.name.contains(query, ignoreCase = true) }
            }
            1 -> {
                // Step 1: Select Leagues. Prioritize Hot Leagues (Hot list first!)
                val baseList = masterLeagues.sortedWith(
                    compareByDescending<OnboardingItem> { it.isHot }
                        .thenBy { it.name }
                )
                if (query.isEmpty()) baseList
                else baseList.filter { it.name.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true) }
            }
            2 -> {
                // Step 2: Select Teams. Prioritize clubs belonging to selected leagues (parent league IDs)
                val baseList = masterTeams.sortedWith(
                    compareByDescending<OnboardingItem> { selLeagues.contains(it.parentId) }
                        .thenBy { it.name }
                )
                if (query.isEmpty()) baseList
                else baseList.filter { it.name.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true) }
            }
            3 -> {
                // Step 3: Select Players. Prioritize players playing for selected clubs (parent team IDs)
                val baseList = masterPlayers.sortedWith(
                    compareByDescending<OnboardingItem> { selTeams.contains(it.parentId) }
                        .thenBy { it.name }
                )
                if (query.isEmpty()) baseList
                else baseList.filter { it.name.contains(query, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(itemId: Int) {
        when (_currentStep.value) {
            0 -> {
                val langName = masterLanguages.find { it.id == itemId }?.name ?: "English"
                _selectedLanguage.value = langName
            }
            1 -> {
                val current = _selectedLeagues.value.toMutableSet()
                if (current.contains(itemId)) current.remove(itemId)
                else current.add(itemId)
                _selectedLeagues.value = current
            }
            2 -> {
                val current = _selectedTeams.value.toMutableSet()
                if (current.contains(itemId)) current.remove(itemId)
                else current.add(itemId)
                _selectedTeams.value = current
            }
            3 -> {
                val current = _selectedPlayers.value.toMutableSet()
                if (current.contains(itemId)) current.remove(itemId)
                else current.add(itemId)
                _selectedPlayers.value = current
            }
        }
    }

    fun isSelected(itemId: Int, name: String = ""): Boolean {
        return when (_currentStep.value) {
            0 -> _selectedLanguage.value == name
            1 -> _selectedLeagues.value.contains(itemId)
            2 -> _selectedTeams.value.contains(itemId)
            3 -> _selectedPlayers.value.contains(itemId)
            else -> false
        }
    }

    fun nextStep() {
        val next = _currentStep.value + 1
        if (next > 3) {
            finishOnboarding()
        } else {
            _currentStep.value = next
            _searchQuery.value = "" // Reset search bar text on step transition
        }
    }

    fun prevStep() {
        val prev = _currentStep.value - 1
        if (prev >= 0) {
            _currentStep.value = prev
            _searchQuery.value = "" // Reset search bar text on step transition
        }
    }

    fun skipOnboarding() {
        // Skip from any screen: Save language (use whatever is selected or English), and exit
        viewModelScope.launch {
            // Apply language preference
            onboardingPrefs.edit()
                .putString("selected_language", _selectedLanguage.value)
                .putBoolean("onboarding_completed", true)
                .apply()
            
            _onboardingCompleted.emit(true)
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            // 1. Persist Selected Language
            onboardingPrefs.edit()
                .putString("selected_language", _selectedLanguage.value)
                .putBoolean("onboarding_completed", true)
                .apply()

            // 2. Persist Favorite Leagues
            _selectedLeagues.value.forEach { id ->
                val item = masterLeagues.find { it.id == id }
                if (item != null) {
                    favoriteManager.toggleLeagueFavorite(item.id, item.name, item.logo, item.subtitle)
                }
            }

            // 3. Persist Favorite Teams
            _selectedTeams.value.forEach { id ->
                val item = masterTeams.find { it.id == id }
                if (item != null) {
                    favoriteManager.toggleTeamFavorite(item.id, item.name, item.logo)
                }
            }

            // 4. Persist Favorite Players
            _selectedPlayers.value.forEach { id ->
                favoriteManager.togglePlayerFavorite(id)
            }

            // Notify activity of completion
            _onboardingCompleted.emit(true)
        }
    }
}
