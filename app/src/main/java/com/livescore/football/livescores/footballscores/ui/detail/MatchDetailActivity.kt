package com.livescore.football.livescores.footballscores.ui.detail

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
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
                    if (isTaskRoot) {
                        val intent = Intent(this@MatchDetailActivity, com.livescore.football.livescores.footballscores.ui.main.MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
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
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnShare.setOnClickListener {
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

        // Main tabs selection setup
        binding.btnTabMatch.setOnClickListener { switchMainTab(0, matchId) }
        binding.btnTabPrediction.setOnClickListener { switchMainTab(1, matchId) }

        // Setup prediction lock overlay button click and localization
        binding.layoutPredictionLockOverlay.tvLockTitle.text = getString(R.string.prediction_locked_title)
        binding.layoutPredictionLockOverlay.tvLockSubtitle.text = getString(R.string.prediction_locked_desc)
        binding.layoutPredictionLockOverlay.btnUnlockNow.setOnClickListener {
            startActivity(Intent(this, com.livescore.football.livescores.footballscores.ui.iap.IAPActivity::class.java))
        }

        // Select the default main tab
        val selectPrediction = intent.getBooleanExtra("SELECT_PREDICTION_TAB", false)
        if (selectPrediction) {
            switchMainTab(1, matchId)
        } else {
            switchMainTab(0, matchId)
        }
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

        val activeTextColor = Color.WHITE
        val mutedTextColor = ContextCompat.getColor(this, R.color.text_muted)
        val activeBgColor = ContextCompat.getColor(this, R.color.accent_green)
        val transparentBg = Color.TRANSPARENT

        binding.btnTabStats.setTextColor(if (tabIndex == 0) activeTextColor else mutedTextColor)
        binding.btnTabStats.setTypeface(null, if (tabIndex == 0) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.btnTabStats.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tabIndex == 0) activeBgColor else transparentBg
        )

        binding.btnTabTimeline.setTextColor(if (tabIndex == 1) activeTextColor else mutedTextColor)
        binding.btnTabTimeline.setTypeface(null, if (tabIndex == 1) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.btnTabTimeline.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tabIndex == 1) activeBgColor else transparentBg
        )

        binding.btnTabLineups.setTextColor(if (tabIndex == 2) activeTextColor else mutedTextColor)
        binding.btnTabLineups.setTypeface(null, if (tabIndex == 2) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.btnTabLineups.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tabIndex == 2) activeBgColor else transparentBg
        )

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
                                    "'" + getString(R.string.today) + ",' HH:mm"
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

                        // Bind prediction data and loading state
                        binding.progressPredictionLoading.isVisible = state.isPredictionLoading
                        if (state.isPredictionLoading) {
                            binding.layoutMatchPrediction.root.visibility = View.GONE
                        } else {
                            binding.layoutMatchPrediction.root.visibility = if (state.prediction != null) View.VISIBLE else View.GONE
                            state.prediction?.let { prediction ->
                                bindPredictionData(prediction)
                            }
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

    private fun switchMainTab(tabIndex: Int, matchId: Int) {
        val activeTextColor = Color.WHITE
        val mutedTextColor = ContextCompat.getColor(this, R.color.text_muted)
        val activeBgColor = ContextCompat.getColor(this, R.color.accent_green)
        val transparentBg = Color.TRANSPARENT

        binding.btnTabMatch.setTextColor(if (tabIndex == 0) activeTextColor else mutedTextColor)
        binding.btnTabMatch.setTypeface(null, if (tabIndex == 0) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.btnTabMatch.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tabIndex == 0) activeBgColor else transparentBg
        )

        binding.btnTabPrediction.setTextColor(if (tabIndex == 1) activeTextColor else mutedTextColor)
        binding.btnTabPrediction.setTypeface(null, if (tabIndex == 1) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.btnTabPrediction.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (tabIndex == 1) activeBgColor else transparentBg
        )

        binding.layoutMatchContent.isVisible = tabIndex == 0
        binding.layoutPredictionContent.isVisible = tabIndex == 1
        binding.matchHeader.root.isVisible = tabIndex == 0

        if (tabIndex == 1) {
            binding.layoutMatchPrediction.root.isVisible = true
            binding.layoutPredictionLockOverlay.root.isVisible = false

            val savedLang = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(this)
            val lang = if (savedLang == "vi") "vi" else "en"
            viewModel.fetchAiPrediction(matchId, lang)
        }
    }

    private fun bindPredictionData(prediction: com.livescore.football.livescores.footballscores.data.remote.model.PredictionDataDto) {
        val detail = viewModel.uiState.value.detail

        val isPremium = limitManager.isPremium()
        
        // Adjust locked section layout height and overlay visibility depending on premium state
        val params = binding.layoutMatchPrediction.layoutPremiumLockedSection.layoutParams
        if (isPremium) {
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            binding.layoutMatchPrediction.layoutPremiumBlurOverlay.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.layoutMatchPrediction.layoutPremiumLockedSection.setRenderEffect(null)
            }
        } else {
            // Clip height to 140dp to show a preview starting from AI Confidence that fades/blurs out heavily
            params.height = (140 * resources.displayMetrics.density).toInt()
            binding.layoutMatchPrediction.layoutPremiumBlurOverlay.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createBlurEffect(
                    12f,
                    12f,
                    Shader.TileMode.CLAMP
                )
                binding.layoutMatchPrediction.layoutPremiumLockedSection.setRenderEffect(blurEffect)
            }
            
            // Set up click listener on "See More" button to display the custom Dialog popup
            binding.layoutMatchPrediction.btnPredictionSeeMore.setOnClickListener {
                val existing = supportFragmentManager.findFragmentByTag(com.livescore.football.livescores.footballscores.ui.custom.AiPredictionPaywallDialog.TAG)
                if (existing == null) {
                    val dialog = com.livescore.football.livescores.footballscores.ui.custom.AiPredictionPaywallDialog.newInstance()
                    dialog.show(supportFragmentManager, com.livescore.football.livescores.footballscores.ui.custom.AiPredictionPaywallDialog.TAG)
                }
            }
        }
        binding.layoutMatchPrediction.layoutPremiumLockedSection.layoutParams = params

        // 1. Team Logos & Names
        val homeTeamName = detail?.teams?.home?.name ?: prediction.home_team.name
        val awayTeamName = detail?.teams?.away?.name ?: prediction.away_team.name

        if (detail != null) {
            Glide.with(this).load(detail.teams.home.logo).into(binding.layoutMatchPrediction.ivPredictHomeLogo)
            Glide.with(this).load(detail.teams.away.logo).into(binding.layoutMatchPrediction.ivPredictAwayLogo)
            binding.layoutMatchPrediction.tvPredictHomeName.text = detail.teams.home.name
            binding.layoutMatchPrediction.tvPredictAwayName.text = detail.teams.away.name
        } else {
            Glide.with(this).load(prediction.home_team.logo).into(binding.layoutMatchPrediction.ivPredictHomeLogo)
            Glide.with(this).load(prediction.away_team.logo).into(binding.layoutMatchPrediction.ivPredictAwayLogo)
            binding.layoutMatchPrediction.tvPredictHomeName.text = prediction.home_team.name
            binding.layoutMatchPrediction.tvPredictAwayName.text = prediction.away_team.name
        }

        // Highlight Predicted Winner Name
        val homeWinner = prediction.winner_prediction == "home"
        val awayWinner = prediction.winner_prediction == "away"

        binding.layoutMatchPrediction.tvPredictHomeName.setTextColor(
            if (homeWinner) ContextCompat.getColor(this, R.color.accent_green) else ContextCompat.getColor(this, R.color.text_white)
        )
        binding.layoutMatchPrediction.tvPredictAwayName.setTextColor(
            if (awayWinner) ContextCompat.getColor(this, R.color.accent_green) else ContextCompat.getColor(this, R.color.text_white)
        )

        // 2. Score Prediction & Confidence
        val homeScore = prediction.score_prediction?.home ?: 0
        val awayScore = prediction.score_prediction?.away ?: 0
        binding.layoutMatchPrediction.tvPredictScore.text = "$homeScore - $awayScore"
        binding.layoutMatchPrediction.tvPredictConfidence.text = "${prediction.confidence_score ?: 0}%"
        binding.layoutMatchPrediction.progressConfidence.progress = prediction.confidence_score ?: 0

        // 3. Over / Under & BTTS
        binding.layoutMatchPrediction.tvPredictOverUnder.text = prediction.over_under_prediction ?: ""

        val isBtts = prediction.btts_prediction == true
        binding.layoutMatchPrediction.tvPredictBtts.text = if (isBtts) getString(R.string.ai_yes) else getString(R.string.ai_no)
        binding.layoutMatchPrediction.tvPredictBtts.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, if (isBtts) R.color.accent_green else R.color.primaryRed)
        )
        binding.layoutMatchPrediction.tvPredictBtts.setTextColor(
            ContextCompat.getColor(this, if (isBtts) R.color.bg_light else R.color.text_white)
        )

        // 4. Prominent Players
        binding.layoutMatchPrediction.layoutGoalscorersContainer.removeAllViews()
        val prominentPlayers = prediction.prominent_players ?: emptyList()
        if (prominentPlayers.isEmpty()) {
            binding.layoutMatchPrediction.cardGoalscorers.visibility = View.GONE
        } else {
            binding.layoutMatchPrediction.cardGoalscorers.visibility = View.VISIBLE
            for (player in prominentPlayers) {
                val scorerBinding = com.livescore.football.livescores.footballscores.databinding.ItemPredictedGoalscorerBinding.inflate(
                    layoutInflater,
                    binding.layoutMatchPrediction.layoutGoalscorersContainer,
                    false
                )
                scorerBinding.tvPlayerName.text = player.player_name
                scorerBinding.tvTeamBadge.text = if (player.team == "home") getString(R.string.ai_home_badge) else getString(R.string.ai_away_badge)
                scorerBinding.tvTeamBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (player.team == "home") ContextCompat.getColor(this, R.color.accent_green) else ContextCompat.getColor(this, R.color.accent_green_secondary)
                )
                scorerBinding.tvProbability.text = "${player.probability}%"
                scorerBinding.progressProbability.progress = player.probability

                if (!player.reason.isNullOrEmpty()) {
                    scorerBinding.tvPlayerReason.text = player.reason
                    scorerBinding.tvPlayerReason.visibility = View.VISIBLE
                } else {
                    scorerBinding.tvPlayerReason.visibility = View.GONE
                }

                binding.layoutMatchPrediction.layoutGoalscorersContainer.addView(scorerBinding.root)
            }
        }

        // 5. Match Analysis & Squad Impact
        binding.layoutMatchPrediction.tvAnalysisText.text = prediction.form_overview ?: ""

        val hasSquadImpact = !prediction.squad_impact.isNullOrEmpty()
        binding.layoutMatchPrediction.layoutSquadImpact.isVisible = hasSquadImpact
        if (hasSquadImpact) {
            binding.layoutMatchPrediction.tvSquadImpactText.text = prediction.squad_impact
        }

        // 6. Tactical Analysis & Strengths/Weaknesses
        binding.layoutMatchPrediction.tvTacticalAnalysisText.text = prediction.tactical_analysis ?: ""
        
        binding.layoutMatchPrediction.tvTacticalAnalysisText.maxLines = Integer.MAX_VALUE
        binding.layoutMatchPrediction.tvReadMoreTactics.visibility = View.GONE

        // Populate Strengths & Weaknesses side-by-side
        binding.layoutMatchPrediction.tvHomeTeamSWLabel.text = homeTeamName.uppercase()
        binding.layoutMatchPrediction.tvAwayTeamSWLabel.text = awayTeamName.uppercase()

        binding.layoutMatchPrediction.layoutHomeSWContainer.removeAllViews()
        binding.layoutMatchPrediction.layoutAwaySWContainer.removeAllViews()

        val sw = prediction.strengths_weaknesses
        if (sw != null) {
            fun addBulletPoints(container: android.widget.LinearLayout, items: List<String>?, isStrength: Boolean) {
                if (items.isNullOrEmpty()) return
                val prefix = if (isStrength) "<font color='#00C853'>✓</font>  " else "<font color='#FF1744'>✗</font>  "
                for (item in items) {
                    val tv = android.widget.TextView(this).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 6.dpToPx())
                        }
                        text = android.text.Html.fromHtml(prefix + item, android.text.Html.FROM_HTML_MODE_LEGACY)
                        setTextColor(ContextCompat.getColor(this@MatchDetailActivity, R.color.text_white))
                        textSize = 12f
                        setLineSpacing(0f, 1.15f)
                    }
                    container.addView(tv)
                }
            }

            addBulletPoints(binding.layoutMatchPrediction.layoutHomeSWContainer, sw.home_strengths, true)
            addBulletPoints(binding.layoutMatchPrediction.layoutHomeSWContainer, sw.home_weaknesses, false)
            addBulletPoints(binding.layoutMatchPrediction.layoutAwaySWContainer, sw.away_strengths, true)
            addBulletPoints(binding.layoutMatchPrediction.layoutAwaySWContainer, sw.away_weaknesses, false)
        }

        // 7. Key Stats, Corners & Cards
        val cornersHome = prediction.corners_prediction?.home ?: 0
        val cornersAway = prediction.corners_prediction?.away ?: 0
        binding.layoutMatchPrediction.tvPredictCorners.text = "$cornersHome - $cornersAway"

        val cardsHome = prediction.yellow_cards_prediction?.home ?: 0
        val cardsAway = prediction.yellow_cards_prediction?.away ?: 0
        binding.layoutMatchPrediction.tvPredictYellowCards.text = "$cardsHome - $cardsAway"

        // Key Stats bullets with ball icon emoji
        binding.layoutMatchPrediction.layoutKeyStatsContainer.removeAllViews()
        val keyStats = prediction.key_stats ?: emptyList()
        for (stat in keyStats) {
            val tv = android.widget.TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8.dpToPx())
                }
                text = "⚽  $stat"
                setTextColor(ContextCompat.getColor(this@MatchDetailActivity, R.color.text_white))
                textSize = 12f
                setLineSpacing(0f, 1.15f)
            }
            binding.layoutMatchPrediction.layoutKeyStatsContainer.addView(tv)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDetailPolling()
    }
}
