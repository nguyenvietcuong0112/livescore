package com.livescore.football.livescores.footballscores.ui.wc26

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.repository.LeaguesRepository
import com.livescore.football.livescores.footballscores.databinding.FragmentWc26Binding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.livescore.football.livescores.footballscores.data.local.FavoriteManager
import com.livescore.football.livescores.footballscores.data.local.MatchReminderManager
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.graphics.Rect
import android.view.ViewTreeObserver

@AndroidEntryPoint
class WC26Fragment : Fragment() {

    private var _binding: FragmentWc26Binding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var leaguesRepository: LeaguesRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var reminderManager: MatchReminderManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    private var selectedTab = 0
    private var isWcBracketFullScreen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWc26Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCountdown()
        setupListeners()
        updateTabUI()
        setupAdScrollListeners()
        populateWcTournamentData()
    }

    private fun populateWcTournamentData() {
        pendingAdLoads.clear()
        lifecycleScope.launch {
            try {
                binding.layoutLoadingOverlay.isVisible = true
                binding.btnWcZoom.isVisible = false
                binding.scrollWcFixtures.isVisible = false
                binding.scrollWcGroups.isVisible = false
                binding.scrollWcBracket.isVisible = false

                val inflater = LayoutInflater.from(requireContext())

                // 1. Fetch and Populate Standings (Groups / BXH)
                var standingsList = emptyList<com.livescore.football.livescores.footballscores.data.remote.model.StandingRowDto>()
                
                try {
                    leaguesRepository.getStandings(1, 2026)
                        .collect { list ->
                            standingsList = list
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (standingsList.isNotEmpty()) {
                    binding.layoutWcGroupsContainer.removeAllViews()
                    
                    val grouped = standingsList.groupBy { it.group }
                    val sortedGroups = grouped.entries.sortedBy { it.key }
                    
                    sortedGroups.forEachIndexed { index, entry ->
                        val groupName = entry.key
                        val rows = entry.value.sortedBy { it.rank }
                        
                        val groupView = inflater.inflate(R.layout.item_wc_group, binding.layoutWcGroupsContainer, false)
                        
                        // Keep group name in English: e.g. "GROUP A"
                        val displayGroupName = groupName?.replace("Group", "GROUP")?.uppercase()
                        groupView.findViewById<android.widget.TextView>(R.id.tvGroupName).text = displayGroupName
                        
                        val rowsContainer = groupView.findViewById<android.widget.LinearLayout>(R.id.layoutTeamRowsContainer)
                        rowsContainer.removeAllViews()
                        
                        rows.forEach { row ->
                            val rowView = inflater.inflate(R.layout.item_wc_group_row, rowsContainer, false)
                            
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowRank).text = row.rank.toString()
                            
                            val ivFlag = rowView.findViewById<android.widget.ImageView>(R.id.ivRowFlag)
                            Glide.with(rowView.context)
                                .load(row.team.logo)
                                .placeholder(R.drawable.ic_favorite_border)
                                .into(ivFlag)
                                
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowName).text = row.team.name
                            
                            // Stats columns
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowPlayed).text = (row.all?.played ?: 0).toString()
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowWon).text = (row.all?.win ?: 0).toString()
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowDrawn).text = (row.all?.draw ?: 0).toString()
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowLost).text = (row.all?.lose ?: 0).toString()
                            
                            // GD
                            val gd = row.goalsDiff ?: 0
                            val gdText = if (gd > 0) "+$gd" else gd.toString()
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowGD).text = gdText
                            
                            // PTS
                            rowView.findViewById<android.widget.TextView>(R.id.tvRowPTS).text = row.points.toString()
                            
                            rowsContainer.addView(rowView)
                        }
                        
                        binding.layoutWcGroupsContainer.addView(groupView)

                        if (!limitManager.isPremium() && (index + 1) % 2 == 0) {
                            val adViewWrapper = inflater.inflate(R.layout.layout_native_no_media, binding.layoutWcGroupsContainer, false)
                            adViewWrapper.visibility = View.INVISIBLE
                            binding.layoutWcGroupsContainer.addView(adViewWrapper)
                            val adId = try { getString(resources.getIdentifier("native_all", "string", requireContext().packageName)) } catch (e: Exception) { "" }
                            if (adId.isNotEmpty()) {
                                pendingAdLoads.add(PendingAdLoad(adViewWrapper, adId))
                            } else {
                                adViewWrapper.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    binding.layoutWcGroupsContainer.removeAllViews()
                    val emptyStateView = com.livescore.football.livescores.footballscores.ui.custom.EmptyStateView(requireContext()).apply {
                        text = getString(R.string.empty_fixtures)
                        setPadding(0, dpToPx(32f), 0, 0)
                    }
                    binding.layoutWcGroupsContainer.addView(emptyStateView)
                }

                // 2. Fetch and Populate Fixtures & Bracket (Trận đấu & VLTT)
                var fixturesList = emptyList<com.livescore.football.livescores.footballscores.data.remote.model.MatchItemDto>()
                try {
                    leaguesRepository.getFixturesByLeague(1, 2026)
                        .collect { list ->
                            fixturesList = list
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                if (fixturesList.isNotEmpty()) {
                    binding.layoutWcFixturesContainer.removeAllViews()
                    
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val localDateKeyFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    val localTimeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    
                    val groupedMatches = fixturesList.groupBy { match ->
                        try {
                            val dateObj = parser.parse(match.fixture.date)
                            if (dateObj != null) localDateKeyFormat.format(dateObj) else "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }.entries.sortedBy { it.key }
                    
                    val locale = java.util.Locale.getDefault()
                    val headerDateFormat = if (locale.language == "vi") {
                        java.text.SimpleDateFormat("'Ngày' dd 'tháng' MM 'năm' yyyy", locale)
                    } else {
                        java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", locale)
                    }.apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    
                    var matchCount = 0
                    
                    groupedMatches.forEach { entry ->
                        val dateKeyStr = entry.key
                        val matchesForDate = entry.value
                        
                        if (dateKeyStr != "Unknown") {
                            val headerDateObj = localDateKeyFormat.parse(dateKeyStr)
                            if (headerDateObj != null) {
                                val headerView = inflater.inflate(R.layout.item_wc_date_header, binding.layoutWcFixturesContainer, false)
                                headerView.findViewById<android.widget.TextView>(R.id.tvDateHeader).text = headerDateFormat.format(headerDateObj)
                                binding.layoutWcFixturesContainer.addView(headerView)
                            }
                        }
                        
                        matchesForDate.forEach { match ->
                            val fixtureView = inflater.inflate(R.layout.item_wc_fixture, binding.layoutWcFixturesContainer, false)
                            
                            val groupStageString = getString(R.string.wc_group_stage_name)
                            val rawRound = match.league.round ?: groupStageString
                            val displayRound = rawRound.replace("Group Stage", groupStageString).uppercase()
                            fixtureView.findViewById<android.widget.TextView>(R.id.tvFixtureRound).text = displayRound
                            
                            val venueName = match.fixture.venue?.name ?: getString(R.string.wc_default_stadium)
                            val venueCity = match.fixture.venue?.city ?: ""
                            fixtureView.findViewById<android.widget.TextView>(R.id.tvFixtureStadium).text = "$venueName, $venueCity"
                            
                            fixtureView.findViewById<android.widget.TextView>(R.id.tvFixtureTeam1).text = match.teams.home.name
                            fixtureView.findViewById<android.widget.TextView>(R.id.tvFixtureTeam2).text = match.teams.away.name
                            
                            val ivHomeLogo = fixtureView.findViewById<android.widget.ImageView>(R.id.ivFixtureHomeLogo)
                            val ivAwayLogo = fixtureView.findViewById<android.widget.ImageView>(R.id.ivFixtureAwayLogo)
                            
                            Glide.with(fixtureView.context)
                                .load(match.teams.home.logo)
                                .placeholder(R.drawable.ic_favorite_border)
                                .into(ivHomeLogo)
                            ivHomeLogo.imageTintList = null

                            Glide.with(fixtureView.context)
                                .load(match.teams.away.logo)
                                .placeholder(R.drawable.ic_favorite_border)
                                .into(ivAwayLogo)
                            ivAwayLogo.imageTintList = null

                            fixtureView.setOnClickListener {
                                val intent = android.content.Intent(requireContext(), com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity::class.java).apply {
                                    putExtra("MATCH_ID", match.fixture.id)
                                    putExtra("HOME_TEAM", match.teams.home.name)
                                    putExtra("AWAY_TEAM", match.teams.away.name)
                                }
                                startActivity(intent)
                            }
                            
                            val kickoffTime = try {
                                val dateObj = parser.parse(match.fixture.date)
                                if (dateObj != null) localTimeFormat.format(dateObj) else ""
                            } catch (e: Exception) {
                                ""
                            }
                            
                            val homeGoal = match.goals.home
                            val awayGoal = match.goals.away
                            val tvVS = fixtureView.findViewById<android.widget.TextView>(R.id.tvVS)
                            val tvFixtureTime = fixtureView.findViewById<android.widget.TextView>(R.id.tvFixtureTime)
                            
                            if (homeGoal != null && awayGoal != null) {
                                tvVS.text = "$homeGoal - $awayGoal"
                                tvVS.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                                tvFixtureTime.text = if (kickoffTime.isNotEmpty()) "$kickoffTime • ${match.fixture.status.short}" else match.fixture.status.short
                            } else {
                                tvVS.text = "VS"
                                tvVS.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                                tvFixtureTime.text = kickoffTime
                            }
                            
                            val ivReminder = fixtureView.findViewById<android.widget.ImageView>(R.id.ivReminder)
                            val ivFavorite = fixtureView.findViewById<android.widget.ImageView>(R.id.ivFavorite)
                            
                            val isUpcoming = match.fixture.status.short == "NS" || match.fixture.status.short == "TBD"
                            val isUpcomingFuture = isUpcoming && (match.fixture.timestamp * 1000 > System.currentTimeMillis())
                            ivReminder.visibility = if (isUpcomingFuture) View.VISIBLE else View.GONE
                            
                            val isRemind = reminderManager.isReminderSet(match.fixture.id)
                            ivReminder.setImageResource(if (isRemind) R.drawable.ic_bell_active else R.drawable.ic_bell)
                            ivReminder.setColorFilter(ContextCompat.getColor(requireContext(), if (isRemind) R.color.accent_green else R.color.text_muted))
                            
                            val isFav = favoriteManager.isFixtureFavorite(match.fixture.id)
                            ivFavorite.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                            ivFavorite.setColorFilter(ContextCompat.getColor(requireContext(), if (isFav) R.color.accent_green else R.color.text_muted))
                            
                            ivReminder.setOnClickListener {
                                val matchEntity = CachedMatchEntity(
                                    id = match.fixture.id,
                                    leagueId = match.league.id,
                                    leagueName = match.league.name,
                                    leagueLogo = match.league.logo,
                                    homeTeamId = match.teams.home.id,
                                    homeTeamName = match.teams.home.name,
                                    homeTeamLogo = match.teams.home.logo,
                                    awayTeamId = match.teams.away.id,
                                    awayTeamName = match.teams.away.name,
                                    awayTeamLogo = match.teams.away.logo,
                                    statusShort = match.fixture.status.short,
                                    elapsed = match.fixture.status.elapsed,
                                    goalsHome = match.goals.home,
                                    goalsAway = match.goals.away,
                                    dateTimestamp = match.fixture.timestamp,
                                    statusLong = match.fixture.status.long,
                                    queryDate = ""
                                )
                                val newRemind = reminderManager.toggleReminder(matchEntity)
                                ivReminder.setImageResource(if (newRemind) R.drawable.ic_bell_active else R.drawable.ic_bell)
                                ivReminder.setColorFilter(ContextCompat.getColor(requireContext(), if (newRemind) R.color.accent_green else R.color.text_muted))
                                val msg = if (newRemind) getString(R.string.reminder_set_toast) else getString(R.string.reminder_cancelled_toast)
                                android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            
                            ivFavorite.setOnClickListener {
                                if (favoriteManager.canAddFavoriteFixture(match.fixture.id)) {
                                    val newFav = favoriteManager.toggleFixtureFavorite(match.fixture.id)
                                    ivFavorite.setImageResource(if (newFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                                    ivFavorite.setColorFilter(ContextCompat.getColor(requireContext(), if (newFav) R.color.accent_green else R.color.text_muted))
                                } else {
                                    val paywall = com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog.newInstance()
                                    paywall.show(parentFragmentManager, com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog.TAG)
                                }
                            }
                            
                            binding.layoutWcFixturesContainer.addView(fixtureView)
                            matchCount++
                            
                            if (!limitManager.isPremium() && matchCount % 3 == 0) {
                                val adViewWrapper = inflater.inflate(R.layout.layout_native_no_media, binding.layoutWcFixturesContainer, false)
                                adViewWrapper.visibility = View.INVISIBLE
                                binding.layoutWcFixturesContainer.addView(adViewWrapper)
                                val adId = try { getString(resources.getIdentifier("native_all", "string", requireContext().packageName)) } catch (e: Exception) { "" }
                                if (adId.isNotEmpty()) {
                                    pendingAdLoads.add(PendingAdLoad(adViewWrapper, adId))
                                } else {
                                    adViewWrapper.visibility = View.GONE
                                }
                            }
                        }
                    }
                } else {
                    binding.layoutWcFixturesContainer.removeAllViews()
                    val emptyStateView = com.livescore.football.livescores.footballscores.ui.custom.EmptyStateView(requireContext()).apply {
                        text = getString(R.string.empty_fixtures)
                        setPadding(0, dpToPx(32f), 0, 0)
                    }
                    binding.layoutWcFixturesContainer.addView(emptyStateView)
                }

                // 3. Populate Bracket (VLTT) View
                val bracketMatches = fixturesList.filter { match ->
                    val round = match.league.round?.lowercase() ?: ""
                    round.contains("round of 16") || round.contains("quarter-finals") || round.contains("semi-finals") || (round.contains("final") && !round.contains("third"))
                }

                val r16List = bracketMatches.filter { it.league.round?.lowercase()?.contains("round of 16") == true }.sortedBy { it.fixture.timestamp }
                val qfList = bracketMatches.filter { it.league.round?.lowercase()?.contains("quarter-finals") == true }.sortedBy { it.fixture.timestamp }
                val sfList = bracketMatches.filter { it.league.round?.lowercase()?.contains("semi-finals") == true }.sortedBy { it.fixture.timestamp }
                val finalMatch = bracketMatches.firstOrNull { it.league.round?.lowercase()?.contains("final") == true && !it.league.round.lowercase().contains("third") }

                binding.colR16.removeAllViews()
                binding.colQF.removeAllViews()
                binding.colSF.removeAllViews()
                binding.colFinal.removeAllViews()
                binding.connCol1.removeAllViews()
                binding.connCol2.removeAllViews()
                binding.connCol3.removeAllViews()

                // Render Column 1: Round of 16 (8 matches)
                val r16Dates = listOf(
                    "29/6, 02:00", "30/6, 08:00", "30/6, 03:30", "1/7, 04:00",
                    "2/7, 03:00", "2/7, 07:00", "3/7, 02:00", "3/7, 06:00"
                )
                for (i in 0 until 8) {
                    if (i > 0) addSpacer(binding.colR16, 16f)
                    val match = r16List.getOrNull(i)
                    addBracketMatch(binding.colR16, match, r16Dates[i])
                }

                // Render Connector 1: R16 -> QF
                addSpacer(binding.connCol1, 50f)
                for (i in 0 until 4) {
                    if (i > 0) addSpacer(binding.connCol1, 116f)
                    addConnector(binding.connCol1, 116f)
                }

                // Render Column 2: Quarterfinals (4 matches)
                val qfDates = listOf("5/7, 00:00", "5/7, 04:00", "7/7, 07:00", "7/7, 11:00")
                addSpacer(binding.colQF, 58f)
                for (i in 0 until 4) {
                    if (i > 0) addSpacer(binding.colQF, 132f)
                    val match = qfList.getOrNull(i)
                    addBracketMatch(binding.colQF, match, qfDates[i])
                }

                // Render Connector 2: QF -> SF
                addSpacer(binding.connCol2, 108f)
                for (i in 0 until 2) {
                    if (i > 0) addSpacer(binding.connCol2, 232f)
                    addConnector(binding.connCol2, 232f)
                }

                // Render Column 3: Semifinals (2 matches)
                val sfDates = listOf("10/7, 03:00", "10/7, 07:00")
                addSpacer(binding.colSF, 174f)
                for (i in 0 until 2) {
                    if (i > 0) addSpacer(binding.colSF, 364f)
                    val match = sfList.getOrNull(i)
                    addBracketMatch(binding.colSF, match, sfDates[i])
                }

                // Render Connector 3: SF -> Final
                addSpacer(binding.connCol3, 224f)
                addConnector(binding.connCol3, 464f)

                // Render Column 4: Final (1 match)
                addSpacer(binding.colFinal, 406f)
                addBracketMatch(binding.colFinal, finalMatch, "11/7, 02:00")

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (_binding != null) {
                    binding.layoutLoadingOverlay.isVisible = false
                    updateTabUI()
                }
            }
        }
    }

    private fun addBracketMatch(column: ViewGroup, match: com.livescore.football.livescores.footballscores.data.remote.model.MatchItemDto?, defaultDate: String) {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.item_wc_bracket_match, column, false)
        bindBracketMatch(view, match, defaultDate)
        column.addView(view)
    }

    private fun addSpacer(column: ViewGroup, heightDp: Float) {
        val spacer = View(requireContext())
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(heightDp)
        )
        spacer.layoutParams = params
        column.addView(spacer)
    }

    private fun addConnector(column: ViewGroup, heightDp: Float) {
        val view = com.livescore.football.livescores.footballscores.ui.custom.BracketConnectorView(requireContext())
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(heightDp)
        )
        view.layoutParams = params
        column.addView(view)
    }

    private fun bindBracketMatch(
        view: View, 
        match: com.livescore.football.livescores.footballscores.data.remote.model.MatchItemDto?,
        defaultDate: String
    ) {
        val tvDate = view.findViewById<android.widget.TextView>(R.id.tvMatchDate)
        val ivLogo1 = view.findViewById<android.widget.ImageView>(R.id.ivTeam1Logo)
        val tvName1 = view.findViewById<android.widget.TextView>(R.id.tvTeam1Name)
        val tvScore1 = view.findViewById<android.widget.TextView>(R.id.tvTeam1Score)
        val ivLogo2 = view.findViewById<android.widget.ImageView>(R.id.ivTeam2Logo)
        val tvName2 = view.findViewById<android.widget.TextView>(R.id.tvTeam2Name)
        val tvScore2 = view.findViewById<android.widget.TextView>(R.id.tvTeam2Score)

        if (match != null) {
            val rawDate = match.fixture.date
            val displayDate = try {
                val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(rawDate)
                val formatter = java.text.SimpleDateFormat("dd/MM, HH:mm", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getDefault()
                }
                if (date != null) formatter.format(date) else defaultDate
            } catch (e: Exception) {
                defaultDate
            }
            tvDate.text = displayDate

            tvName1.text = match.teams.home.name
            Glide.with(view.context)
                .load(match.teams.home.logo)
                .placeholder(R.drawable.ic_favorite_border)
                .into(ivLogo1)
            ivLogo1.imageTintList = null

            tvName2.text = match.teams.away.name
            Glide.with(view.context)
                .load(match.teams.away.logo)
                .placeholder(R.drawable.ic_favorite_border)
                .into(ivLogo2)
            ivLogo2.imageTintList = null

            val homeGoal = match.goals.home
            val awayGoal = match.goals.away
            if (homeGoal != null && awayGoal != null) {
                tvScore1.text = homeGoal.toString()
                tvScore2.text = awayGoal.toString()
                tvScore1.visibility = View.VISIBLE
                tvScore2.visibility = View.VISIBLE
            } else {
                tvScore1.visibility = View.GONE
                tvScore2.visibility = View.GONE
            }

            view.setOnClickListener {
                val intent = android.content.Intent(view.context, com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity::class.java).apply {
                    putExtra("MATCH_ID", match.fixture.id)
                    putExtra("HOME_TEAM", match.teams.home.name)
                    putExtra("AWAY_TEAM", match.teams.away.name)
                }
                view.context.startActivity(intent)
            }
        } else {
            view.setOnClickListener(null)
            tvDate.text = defaultDate
            tvName1.text = getString(R.string.wc_tbd)
            tvName2.text = getString(R.string.wc_tbd)
            ivLogo1.setImageResource(R.drawable.ic_favorite_border)
            ivLogo1.imageTintList = ContextCompat.getColorStateList(view.context, R.color.text_muted)
            ivLogo2.setImageResource(R.drawable.ic_favorite_border)
            ivLogo2.imageTintList = ContextCompat.getColorStateList(view.context, R.color.text_muted)
            tvScore1.visibility = View.GONE
            tvScore2.visibility = View.GONE
        }
    }

    private fun setupCountdown() {
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 11)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffMs = targetCal.timeInMillis - System.currentTimeMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
        
        val countdownText = if (diffDays > 0) {
            getString(R.string.wc26_days_until, diffDays)
        } else if (diffDays == 0L) {
            getString(R.string.wc26_starts_today)
        } else {
            getString(R.string.wc26_ongoing)
        }
        binding.tvCountdown.text = countdownText
    }

    private fun setupListeners() {
        binding.tabWcFixtures.setOnClickListener {
            selectedTab = 0
            isWcBracketFullScreen = false
            binding.bannerHeader.isVisible = true
            binding.wcTabsContainer.isVisible = true
            binding.btnWcZoom.setIconResource(R.drawable.ic_zoom_in)
            updateTabUI()
        }
        binding.tabWcGroups.setOnClickListener {
            selectedTab = 1
            isWcBracketFullScreen = false
            binding.bannerHeader.isVisible = true
            binding.wcTabsContainer.isVisible = true
            binding.btnWcZoom.setIconResource(R.drawable.ic_zoom_in)
            updateTabUI()
        }
        binding.tabWcVLTT.setOnClickListener {
            selectedTab = 2
            updateTabUI()
        }
        binding.btnWcZoom.setOnClickListener {
            isWcBracketFullScreen = !isWcBracketFullScreen
            binding.bannerHeader.isVisible = !isWcBracketFullScreen
            binding.wcTabsContainer.isVisible = !isWcBracketFullScreen
            binding.btnWcZoom.setIconResource(
                if (isWcBracketFullScreen) R.drawable.ic_zoom_out else R.drawable.ic_zoom_in
            )
        }
    }

    private fun updateTabUI() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        
        binding.tabWcFixtures.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }
        binding.tabWcGroups.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }
        binding.tabWcVLTT.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }

        binding.scrollWcFixtures.isVisible = false
        binding.scrollWcGroups.isVisible = false
        binding.scrollWcBracket.isVisible = false
        binding.btnWcZoom.isVisible = (selectedTab == 2)

        when (selectedTab) {
            0 -> {
                binding.tabWcFixtures.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcFixtures.isVisible = true
            }
            1 -> {
                binding.tabWcGroups.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcGroups.isVisible = true
            }
            2 -> {
                binding.tabWcVLTT.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcBracket.isVisible = true
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private val pendingAdLoads = mutableListOf<PendingAdLoad>()
    private var scrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private data class PendingAdLoad(
        val adViewWrapper: View,
        val adId: String
    )

    private fun setupAdScrollListeners() {
        scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            checkPendingAds()
        }
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            checkPendingAds()
        }
        
        binding.scrollWcFixtures.viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        binding.scrollWcFixtures.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        binding.scrollWcGroups.viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        binding.scrollWcGroups.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun checkPendingAds() {
        if (pendingAdLoads.isEmpty()) return
        val iterator = pendingAdLoads.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (isViewNearScreen(pending.adViewWrapper)) {
                iterator.remove()
                triggerAdLoad(pending)
            }
        }
    }

    private fun isViewNearScreen(view: View): Boolean {
        if (!view.isShown) return false
        if (view.width == 0 || view.height == 0) return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewTop = location[1]
        val screenHeight = resources.displayMetrics.heightPixels
        return viewTop < screenHeight + 500 && viewTop + view.height > -500
    }

    private fun triggerAdLoad(pending: PendingAdLoad) {
        val context = context ?: return
        Admob.getInstance().loadNativeAds(
            context,
            pending.adId,
            1,
            object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: com.google.android.gms.ads.nativead.NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    if (!isAdded) return
                    pending.adViewWrapper.visibility = View.VISIBLE
                    Admob.getInstance().pushAdsToViewCustom(
                        nativeAd,
                        pending.adViewWrapper as NativeAdView
                    )
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    if (!isAdded) return
                    pending.adViewWrapper.visibility = View.GONE
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scrollChangedListener?.let {
            _binding?.scrollWcFixtures?.viewTreeObserver?.removeOnScrollChangedListener(it)
            _binding?.scrollWcGroups?.viewTreeObserver?.removeOnScrollChangedListener(it)
        }
        layoutListener?.let {
            _binding?.scrollWcFixtures?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            _binding?.scrollWcGroups?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
        }
        scrollChangedListener = null
        layoutListener = null
        pendingAdLoads.clear()
        _binding = null
    }
}
