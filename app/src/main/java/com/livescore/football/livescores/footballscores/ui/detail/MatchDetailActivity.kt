package com.livescore.football.livescores.footballscores.ui.detail

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.livescore.football.livescores.footballscores.base.BaseActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.ActivityMatchDetailBinding
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog
import com.livescore.football.livescores.footballscores.ui.custom.TimelineEvent
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class MatchDetailActivity : BaseActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private lateinit var binding: ActivityMatchDetailBinding
    private val viewModel: MatchDetailViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "MatchDetail"
        )
    }

    private lateinit var eventAdapter: EventAdapter
    private lateinit var lineupAdapter: LineupAdapter

    override fun bind() {
        binding = ActivityMatchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup onBackPressed frequency capped interstitial ad (35s)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AdsConfig.showInterClickAd(this@MatchDetailActivity) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        val matchId = intent.getIntExtra("MATCH_ID", -1)
        if (matchId == -1) {
            finish()
            return
        }

        setupUI(matchId)
        setupLockOverlays()
        observeViewModel()
        viewModel.startDetailPolling(matchId)
    }

    private fun setupUI(matchId: Int) {
        binding.matchHeader.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.matchHeader.btnShare.setOnClickListener {
            shareCombinedMatchView(matchId)
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

    private fun captureViewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(this, R.color.background))
        view.draw(canvas)
        return bitmap
    }

    private fun shareMatchView(view: View, matchId: Int) {
        try {
            val bitmap = captureViewToBitmap(view)
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_match.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val detail = viewModel.uiState.value.detail
            val homeName = detail?.teams?.home?.name ?: "Home"
            val awayName = detail?.teams?.away?.name ?: "Away"
            val caption = getString(R.string.share_caption_template, homeName, awayName, matchId)

            val contentUri = FileProvider.getUriForFile(
                this,
                "com.livescore.football.livescores.footballscores.fileprovider",
                file
            )

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, getString(R.string.toast_share_match_failed), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCombinedMatchView(matchId: Int) {
        try {
            val currentStatsVisible = binding.containerStats.isVisible
            val currentTimelineVisible = binding.layoutTimeline.root.isVisible
            val currentLineupsVisible = binding.layoutLineups.root.isVisible

            // Ensure stats layout is visible to perform measurements and drawing
            binding.containerStats.isVisible = true

            // Force layout pass to ensure views have dimensions
            val displayWidth = resources.displayMetrics.widthPixels
            val widthSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

            binding.matchHeader.root.measure(widthSpec, heightSpec)
            binding.matchHeader.root.layout(
                0, 0, binding.matchHeader.root.measuredWidth, binding.matchHeader.root.measuredHeight
            )

            binding.containerStats.measure(widthSpec, heightSpec)
            binding.containerStats.layout(
                0, 0, binding.containerStats.measuredWidth, binding.containerStats.measuredHeight
            )

            val headerWidth = binding.matchHeader.root.measuredWidth
            val headerHeight = binding.matchHeader.root.measuredHeight
            val statsWidth = binding.containerStats.measuredWidth
            val statsHeight = binding.containerStats.measuredHeight

            val totalWidth = maxOf(headerWidth, statsWidth)
            val totalHeight = headerHeight + statsHeight

            if (totalWidth <= 0 || totalHeight <= 0) {
                // Fallback to simple header capture if measurements failed
                binding.containerStats.isVisible = currentStatsVisible
                shareMatchView(binding.matchHeader.root, matchId)
                return
            }

            val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(ContextCompat.getColor(this, R.color.background))

            // 1. Draw Match Header
            binding.matchHeader.root.draw(canvas)

            // 2. Draw Stats View below Match Header
            canvas.save()
            canvas.translate(0f, headerHeight.toFloat())
            binding.containerStats.draw(canvas)
            canvas.restore()

            // Restore original tab visibility states to avoid layout flickering
            binding.containerStats.isVisible = currentStatsVisible
            binding.layoutTimeline.root.isVisible = currentTimelineVisible
            binding.layoutLineups.root.isVisible = currentLineupsVisible

            // Save combined bitmap to cache directory
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_match.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // Generate localized dynamic caption
            val detail = viewModel.uiState.value.detail
            val homeName = detail?.teams?.home?.name ?: "Home"
            val awayName = detail?.teams?.away?.name ?: "Away"
            val caption = getString(R.string.share_caption_template, homeName, awayName, matchId)

            val contentUri = FileProvider.getUriForFile(
                this,
                "com.livescore.football.livescores.footballscores.fileprovider",
                file
            )

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // General fallback
            shareMatchView(binding.root, matchId)
        }
    }

    private fun switchTab(tabIndex: Int) {
        if (!limitManager.isPremium() && (tabIndex == 0 || tabIndex == 2)) {
            if (limitManager.isLimitExceeded() || limitManager.isNearQuotaLimit()) {
                showPremiumPaywall()
                return
            }
        }

        val activeColor = ContextCompat.getColor(this, R.color.accent_green)
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnTabStats.setTextColor(if (tabIndex == 0) activeColor else mutedColor)
        binding.btnTabTimeline.setTextColor(if (tabIndex == 1) activeColor else mutedColor)
        binding.btnTabLineups.setTextColor(if (tabIndex == 2) activeColor else mutedColor)

        binding.containerStats.isVisible = tabIndex == 0
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
                            binding.matchHeader.tvDetailLeague.text = detail.league.name.uppercase()
                            binding.matchHeader.tvDetailHomeName.text = detail.teams.home.name
                            binding.matchHeader.tvDetailAwayName.text = detail.teams.away.name

                            binding.matchHeader.tvDetailScore.text =
                                "${detail.goals.home ?: 0} - ${detail.goals.away ?: 0}"
                            
                            val statusShort = detail.fixture.status.short
                            if (statusShort == "NS" || statusShort == "TBD") {
                                val matchDate = java.util.Date(detail.fixture.timestamp * 1000)
                                val sdfToday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getDefault()
                                }
                                val isToday = sdfToday.format(matchDate) == sdfToday.format(java.util.Date())
                                val locale = java.util.Locale.getDefault()
                                val pattern = if (isToday) {
                                    if (locale.language == "vi") "'Hôm nay,' HH:mm" else "'Today,' HH:mm"
                                } else {
                                    "dd/MM/yyyy HH:mm"
                                }
                                val sdf = java.text.SimpleDateFormat(pattern, locale).apply {
                                    timeZone = java.util.TimeZone.getDefault()
                                }
                                binding.matchHeader.tvDetailStatus.text = sdf.format(matchDate)
                            } else {
                                binding.matchHeader.tvDetailStatus.text =
                                    detail.fixture.status.elapsed?.let { "${it}' LIVE" } ?: detail.fixture.status.long
                            }

                             Glide.with(this@MatchDetailActivity).load(detail.teams.home.logo).into(binding.matchHeader.ivDetailHomeLogo)
                             Glide.with(this@MatchDetailActivity).load(detail.teams.away.logo).into(binding.matchHeader.ivDetailAwayLogo)
 
                             // Setup dynamic values in Football Pitch View
                             binding.pitchTracker.pitchView.setTeamNames(detail.teams.home.name, detail.teams.away.name)
                             
                             val rawHome = detail.teams.home.name.uppercase()
                             binding.pitchTracker.tvHomeLegend.text = if (rawHome.length > 12) {
                                 "${rawHome.take(12)}..."
                             } else {
                                 rawHome
                             }
                             
                             val rawAway = detail.teams.away.name.uppercase()
                             binding.pitchTracker.tvAwayLegend.text = if (rawAway.length > 12) {
                                 "${rawAway.take(12)}..."
                             } else {
                                 rawAway
                             }
                            
                            val elapsed = detail.fixture.status.elapsed ?: 0
                            val period = when (detail.fixture.status.long.uppercase()) {
                                "FIRST HALF", "1H" -> "1st Half"
                                "SECOND HALF", "2H" -> "2nd Half"
                                "HALFTIME", "HT" -> "Halftime"
                                "MATCH FINISHED", "FT" -> "Full Time"
                                else -> "LIVE"
                            }
                            val matchStatusStr = if (elapsed > 45 && detail.fixture.status.long.uppercase().contains("FIRST")) {
                                "$period | 45:00 +00:${String.format("%02d", elapsed - 45)}"
                            } else if (elapsed > 90 && detail.fixture.status.long.uppercase().contains("SECOND")) {
                                "$period | 90:00 +00:${String.format("%02d", elapsed - 90)}"
                            } else {
                                "$period | ${String.format("%02d", elapsed)}:00"
                            }
                            binding.pitchTracker.pitchView.setMatchStatus(matchStatusStr, "${elapsed}'")

                            // Setup visual timeline events list
                            binding.layoutTimeline.timelineVisualView.setMatchMinute(detail.fixture.status.elapsed ?: 90)
                        }

                        // Set statistics values in UI
                        state.stats.let { list ->
                            if (list.size >= 2) {
                                val homeStats = list[0].statistics
                                val awayStats = list[1].statistics

                                fun getStatValue(stats: List<com.livescore.football.livescores.footballscores.data.remote.model.StatEntryDto>, key: String): Int {
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
                            val homeTeamId = state.detail?.teams?.home?.id
                            eventAdapter.submitList(events, homeTeamId)

                            val canvasEvents = events.map {
                                TimelineEvent(
                                    minute = it.time.elapsed,
                                    type = when (it.type.uppercase()) {
                                        "GOAL" -> "GOAL"
                                        "CARD" -> if (it.detail.contains("Red", true)) "CARD_RED" else "CARD_YELLOW"
                                        "SUBST" -> "SUBST"
                                        else -> "GOAL"
                                    },
                                    isHome = it.team.id == homeTeamId
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

                // Observe request limit exceeds inside detail screen and popup paywall instantly
                launch {
                    limitManager.limitExceededFlow.collect {
                        if (!limitManager.isPremium()) {
                            showPremiumPaywall(isOutOfQuota = true)
                        }
                    }
                }
                launch {
                    var wasPremium = limitManager.isPremium()
                    limitManager.isPremiumFlow.collect { isPremium ->
                        if (isPremium && !wasPremium) {
                            recreate()
                        }
                        wasPremium = isPremium
                    }
                }
            }
        }
    }

    private fun setupLockOverlays() {
        // Local in-view lock card overlays are permanently hidden as per user request
        binding.layoutPitchLockOverlay.root.visibility = View.GONE
        binding.layoutStatsLockOverlay.root.visibility = View.GONE

        // Pop up the Bottom Sheet directly if near limit
        if (limitManager.isNearQuotaLimit() || limitManager.isLimitExceeded()) {
            showPremiumPaywall()
        }
    }

    private fun showPremiumPaywall(isOutOfQuota: Boolean = false) {
        if (supportFragmentManager.isStateSaved) return
        val existing = supportFragmentManager.findFragmentByTag(PremiumPaywallDialog.TAG)
        if (existing == null) {
            val paywall = PremiumPaywallDialog.newInstance(isOutOfQuota)
            paywall.show(supportFragmentManager, PremiumPaywallDialog.TAG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDetailPolling()
    }
}
