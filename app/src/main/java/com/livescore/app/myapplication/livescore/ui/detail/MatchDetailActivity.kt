package com.livescore.app.myapplication.livescore.ui.detail

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.databinding.ActivityMatchDetailBinding
import com.livescore.app.myapplication.livescore.ui.custom.TimelineEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MatchDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchDetailBinding
    private val viewModel: MatchDetailViewModel by viewModels()

    private lateinit var eventAdapter: EventAdapter
    private lateinit var lineupAdapter: LineupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val matchId = intent.getIntExtra("MATCH_ID", -1)
        if (matchId == -1) {
            finish()
            return
        }

        setupUI()
        observeViewModel()
        viewModel.startDetailPolling(matchId)
    }

    private fun setupUI() {
        binding.matchHeader.btnBack.setOnClickListener {
            finish()
        }

        // Subtabs selection setup
        binding.btnTabStats.setOnClickListener { switchTab(0) }
        binding.btnTabTimeline.setOnClickListener { switchTab(1) }
        binding.btnTabLineups.setOnClickListener { switchTab(2) }

        // Setup Event list
        eventAdapter = EventAdapter()
        binding.layoutTimeline.rvEventsList.layoutManager = LinearLayoutManager(this)
        binding.layoutTimeline.rvEventsList.adapter = eventAdapter

        // Setup Lineup list
        lineupAdapter = LineupAdapter()
        binding.layoutLineups.rvLineupPlayers.layoutManager = LinearLayoutManager(this)
        binding.layoutLineups.rvLineupPlayers.adapter = lineupAdapter
    }

    private fun switchTab(tabIndex: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.accent_green)
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnTabStats.setTextColor(if (tabIndex == 0) activeColor else mutedColor)
        binding.btnTabTimeline.setTextColor(if (tabIndex == 1) activeColor else mutedColor)
        binding.btnTabLineups.setTextColor(if (tabIndex == 2) activeColor else mutedColor)

        binding.layoutStats.root.isVisible = tabIndex == 0
        binding.layoutTimeline.root.isVisible = tabIndex == 1
        binding.layoutLineups.root.isVisible = tabIndex == 2
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI State containing remote response models
                launch {
                    viewModel.uiState.collect { state ->
                        state.detail?.let { detail ->
                            binding.matchHeader.tvDetailLeague.text = detail.league.name.toUpperCase()
                            binding.matchHeader.tvDetailHomeName.text = detail.teams.home.name
                            binding.matchHeader.tvDetailAwayName.text = detail.teams.away.name

                            binding.matchHeader.tvDetailScore.text =
                                "${detail.goals.home ?: 0} - ${detail.goals.away ?: 0}"
                            binding.matchHeader.tvDetailStatus.text =
                                detail.fixture.status.elapsed?.let { "${it}' LIVE" } ?: detail.fixture.status.long

                            Glide.with(this@MatchDetailActivity).load(detail.teams.home.logo).into(binding.matchHeader.ivDetailHomeLogo)
                            Glide.with(this@MatchDetailActivity).load(detail.teams.away.logo).into(binding.matchHeader.ivDetailAwayLogo)

                            // Setup visual timeline events list
                            binding.layoutTimeline.timelineVisualView.setMatchMinute(detail.fixture.status.elapsed ?: 90)
                        }

                        // Set statistics values in UI
                        state.stats.let { list ->
                            if (list.size >= 2) {
                                val homeStats = list[0].statistics
                                val awayStats = list[1].statistics

                                fun getStatValue(stats: List<com.livescore.app.myapplication.livescore.data.remote.model.StatEntryDto>, key: String): Int {
                                    val entry = stats.find { it.type == key }?.value
                                    return when (entry) {
                                        is Double -> entry.toInt()
                                        is Float -> entry.toInt()
                                        is String -> entry.replace("%", "").toIntOrNull() ?: 0
                                        else -> (entry as? Number)?.toInt() ?: 0
                                    }
                                }

                                val hp = getStatValue(homeStats, "Ball Possession")
                                val ap = getStatValue(awayStats, "Ball Possession")
                                binding.layoutStats.tvStatHomePossession.text = "$hp%"
                                binding.layoutStats.tvStatAwayPossession.text = "$ap%"
                                binding.layoutStats.progressHomePossession.progress = hp
                                binding.layoutStats.progressAwayPossession.progress = ap

                                val hs = getStatValue(homeStats, "Total Shots")
                                val asShots = getStatValue(awayStats, "Total Shots")
                                binding.layoutStats.tvStatHomeShots.text = hs.toString()
                                binding.layoutStats.tvStatAwayShots.text = asShots.toString()
                                binding.layoutStats.progressHomeShots.progress = hs
                                binding.layoutStats.progressAwayShots.progress = asShots

                                val hst = getStatValue(homeStats, "Shots on Target")
                                val ast = getStatValue(awayStats, "Shots on Target")
                                binding.layoutStats.tvStatHomeShotsTarget.text = hst.toString()
                                binding.layoutStats.tvStatAwayShotsTarget.text = ast.toString()
                                binding.layoutStats.progressHomeShotsTarget.progress = hst
                                binding.layoutStats.progressAwayShotsTarget.progress = ast

                                val hc = getStatValue(homeStats, "Corner Kicks")
                                val ac = getStatValue(awayStats, "Corner Kicks")
                                binding.layoutStats.tvStatHomeCorners.text = hc.toString()
                                binding.layoutStats.tvStatAwayCorners.text = ac.toString()
                                binding.layoutStats.progressHomeCorners.progress = hc
                                binding.layoutStats.progressAwayCorners.progress = ac
                            }
                        }

                        // Submit lineups lists
                        state.lineups.let { lineups ->
                            if (lineups.size >= 2) {
                                binding.layoutLineups.tvHomeFormation.text = lineups[0].formation ?: ""
                                binding.layoutLineups.tvAwayFormation.text = lineups[1].formation ?: ""
                                lineupAdapter.submitLineups(lineups[0].startXI, lineups[1].startXI)
                            }
                        }

                        // Set events
                        state.events.let { events ->
                            eventAdapter.submitList(events)

                            // Translate network events to TimelineEvents
                            val canvasEvents = events.map {
                                TimelineEvent(
                                    minute = it.time.elapsed,
                                    type = when (it.type.toUpperCase()) {
                                        "GOAL" -> "GOAL"
                                        "CARD" -> if (it.detail.contains("Red", true)) "CARD_RED" else "CARD_YELLOW"
                                        "SUBST" -> "SUBST"
                                        else -> "GOAL"
                                    },
                                    isHome = it.comments?.contains("Home", true) ?: true
                                )
                            }
                            binding.layoutTimeline.timelineVisualView.setEvents(canvasEvents)
                        }
                    }
                }

                // Observe Canvas simulation parameters
                launch {
                    viewModel.ballPosition.collect { position ->
                        binding.pitchTracker.pitchView.updateBallPosition(position.first, position.second)
                    }
                }

                launch {
                    viewModel.attackState.collect { side ->
                        when (side) {
                            1 -> binding.pitchTracker.pitchView.triggerHomeAttack()
                            2 -> binding.pitchTracker.pitchView.triggerAwayAttack()
                            else -> binding.pitchTracker.pitchView.clearAttack()
                        }
                    }
                }

                launch {
                    viewModel.momentumData.collect { data ->
                        binding.pitchTracker.momentumView.setMomentumData(data)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDetailPolling()
    }
}
