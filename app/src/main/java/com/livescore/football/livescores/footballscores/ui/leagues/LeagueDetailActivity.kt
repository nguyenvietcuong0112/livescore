package com.livescore.football.livescores.footballscores.ui.leagues

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.ActivityLeagueDetailBinding
import com.livescore.football.livescores.footballscores.data.repository.LeaguesRepository
import com.livescore.football.livescores.footballscores.data.repository.MatchRepository
import com.livescore.football.livescores.footballscores.data.local.FavoriteManager
import com.livescore.football.livescores.footballscores.data.local.MatchReminderManager
import com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity
import com.livescore.football.livescores.footballscores.ui.home.MatchAdapter
import com.livescore.football.livescores.footballscores.ui.home.MatchListItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LeagueDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeagueDetailBinding

    @Inject
    lateinit var leaguesRepository: LeaguesRepository

    @Inject
    lateinit var matchRepository: MatchRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var reminderManager: MatchReminderManager

    private lateinit var standingsAdapter: StandingsAdapter
    private lateinit var fixturesAdapter: MatchAdapter

    private var leagueId: Int = 39
    private var selectedTab: Int = 0 // 0: Standings, 1: Fixtures

    private var pendingReminderAction: (() -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingReminderAction?.invoke()
        }
        pendingReminderAction = null
    }

    private fun checkAndRequestNotificationPermission(onGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                pendingReminderAction = onGranted
                notificationPermissionLauncher.launch(permission)
            }
        } else {
            onGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeagueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read extras
        leagueId = intent.getIntExtra("LEAGUE_ID", 39)
        val leagueName = intent.getStringExtra("LEAGUE_NAME") ?: "Giải đấu"
        val leagueLogo = intent.getStringExtra("LEAGUE_LOGO") ?: ""
        val leagueCountry = intent.getStringExtra("LEAGUE_COUNTRY") ?: ""

        // Set up header views
        binding.tvDetailLeagueName.text = leagueName
        binding.tvDetailLeagueCountry.text = leagueCountry
        
        Glide.with(this)
            .load(leagueLogo)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(binding.ivDetailLeagueLogo)

        // Setup views & listeners
        setupRecyclerViews()
        setupListeners()
        updateTabUI()
        
        // Load data
        loadStandings()
        loadFixtures()
    }

    private fun setupRecyclerViews() {
        // Standings
        standingsAdapter = StandingsAdapter { row ->
            // Click team
        }
        binding.rvLeagueDetailStandings.apply {
            layoutManager = LinearLayoutManager(this@LeagueDetailActivity)
            adapter = standingsAdapter
        }

        // Fixtures
        fixturesAdapter = MatchAdapter(
            onMatchClick = { match ->
                val intent = Intent(this, MatchDetailActivity::class.java).apply {
                    putExtra("MATCH_ID", match.id)
                    putExtra("HOME_TEAM", match.homeTeamName)
                    putExtra("AWAY_TEAM", match.awayTeamName)
                }
                startActivity(intent)
            },
            onFavoriteClick = { match ->
                lifecycleScope.launch {
                    favoriteManager.toggleFixtureFavorite(match.id)
                    loadFixtures() // Refresh visual state
                }
            },
            onReminderClick = { match ->
                checkAndRequestNotificationPermission {
                    val isSet = reminderManager.toggleReminder(match)
                    val alertMsg = if (isSet) {
                        getString(R.string.reminder_set_toast)
                    } else {
                        getString(R.string.reminder_cancelled_toast)
                    }
                    android.widget.Toast.makeText(this, alertMsg, android.widget.Toast.LENGTH_SHORT).show()
                    loadFixtures() // Refresh visual state
                }
            }
        )
        binding.rvLeagueDetailFixtures.apply {
            layoutManager = LinearLayoutManager(this@LeagueDetailActivity)
            adapter = fixturesAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.tabStandings.setOnClickListener {
            selectedTab = 0
            updateTabUI()
        }
        binding.tabFixtures.setOnClickListener {
            selectedTab = 1
            updateTabUI()
        }
    }

    private fun updateTabUI() {
        val activeColor = ContextCompat.getColor(this, R.color.accent_green)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_muted)
        
        if (selectedTab == 0) {
            binding.tabStandings.apply {
                strokeColor = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.accent_green)
                strokeWidth = 3
                setTextColor(activeColor)
                backgroundTintList = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.card_dark)
            }
            binding.tabFixtures.apply {
                strokeWidth = 0
                setTextColor(inactiveColor)
                backgroundTintList = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.transparent)
            }
            binding.layoutTableHeader.isVisible = true
            binding.rvLeagueDetailStandings.isVisible = true
            binding.rvLeagueDetailFixtures.isVisible = false
        } else {
            binding.tabFixtures.apply {
                strokeColor = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.accent_green)
                strokeWidth = 3
                setTextColor(activeColor)
                backgroundTintList = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.card_dark)
            }
            binding.tabStandings.apply {
                strokeWidth = 0
                setTextColor(inactiveColor)
                backgroundTintList = ContextCompat.getColorStateList(this@LeagueDetailActivity, R.color.transparent)
            }
            binding.layoutTableHeader.isVisible = false
            binding.rvLeagueDetailStandings.isVisible = false
            binding.rvLeagueDetailFixtures.isVisible = true
        }
    }

    private fun loadStandings() {
        binding.loadingSpinner.isVisible = true
        lifecycleScope.launch {
            // Using 2024 as default season for Free plan access compatibility
            leaguesRepository.getStandings(leagueId, 2024)
                .catch { e ->
                    binding.loadingSpinner.isVisible = false
                    binding.emptyState.isVisible = true
                    binding.emptyState.text = e.message ?: "Failed to load standings"
                }
                .collect { list ->
                    binding.loadingSpinner.isVisible = false
                    binding.emptyState.isVisible = list.isEmpty() && selectedTab == 0
                    standingsAdapter.submitList(list)
                }
        }
    }

    private fun loadFixtures() {
        lifecycleScope.launch {
            matchRepository.allCachedMatches
                .catch { e ->
                    // Handle error silently
                }
                .collect { list ->
                    val filteredList = list.filter { it.leagueId == leagueId }
                    val favIds = favoriteManager.getFavoriteFixtureIds()
                    val items = filteredList.map {
                        val isFav = favIds.contains(it.id.toString())
                        val isRemind = reminderManager.isReminderSet(it.id)
                        MatchListItem.MatchItem(it, isFavorite = isFav, isReminderSet = isRemind)
                    }
                    fixturesAdapter.submitList(items)
                }
        }
    }
}
