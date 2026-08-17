package com.livescore.football.livescores.footballscores.ui.leagues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.FragmentLeaguesBinding
import com.livescore.football.livescores.footballscores.utils.bindScrollableChild
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.ui.team.TeamFixturesActivity
import javax.inject.Inject

@AndroidEntryPoint
class LeaguesFragment : Fragment() {

    companion object {
        private const val ARG_LEAGUE_ID = "arg_league_id"

        fun newInstance(leagueId: Int = -1): LeaguesFragment {
            return LeaguesFragment().apply {
                if (leagueId > 0) {
                    arguments = Bundle().apply { putInt(ARG_LEAGUE_ID, leagueId) }
                }
            }
        }
    }

    @Inject
    lateinit var limitManager: com.livescore.football.livescores.footballscores.data.local.RequestLimitManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private var _binding: FragmentLeaguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaguesViewModel by viewModels()

    private lateinit var leagueSelectorAdapter: LeagueSelectorAdapter
    private lateinit var standingsAdapter: StandingsAdapter
    private lateinit var wcGroupsAdapter: WcGroupsAdapter
    private lateinit var leagueMatchesAdapter: LeagueMatchesAdapter
    private lateinit var topScorersAdapter: TopStatsAdapter
    private lateinit var topAssistsAdapter: TopStatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Leagues"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        binding.swipeRefreshLayout.apply {
            bindScrollableChild { binding.rvLeaguesContent }
            setOnRefreshListener { viewModel.refreshCurrentData() }
        }
        observeViewModel()
        loadNativeAd()

        arguments?.getInt(ARG_LEAGUE_ID, -1)?.takeIf { it > 0 }?.let { leagueId ->
            viewModel.selectLeague(leagueId)
        }
    }

    private fun setupRecyclerViews() {
        // League Selector Adapter
        leagueSelectorAdapter = LeagueSelectorAdapter { leagueItem ->
            viewModel.selectLeague(leagueItem.id)
        }
        binding.rvLeagues.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = leagueSelectorAdapter
        }

        // Standings Adapter
        standingsAdapter = StandingsAdapter { row ->
            TeamFixturesActivity.startActivity(
                context = requireContext(),
                teamId = row.team.id,
                teamName = row.team.name,
                teamLogo = row.team.logo
            )
        }

        // League Matches Adapter
        leagueMatchesAdapter = LeagueMatchesAdapter { matchItem ->
            val intent = android.content.Intent(requireContext(), com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity::class.java).apply {
                putExtra("MATCH_ID", matchItem.fixture.id)
                putExtra("fixture_id", matchItem.fixture.id)
            }
            val act = activity as? androidx.appcompat.app.AppCompatActivity
            if (act != null) {
                com.livescore.football.livescores.footballscores.utils.AdsConfig.showInterClickAd(act) {
                    startActivity(intent)
                }
            } else {
                startActivity(intent)
            }
        }

        // Top Scorers Adapter
        topScorersAdapter = TopStatsAdapter(isAssists = false) { playerItem ->
            val team = playerItem.statistics.firstOrNull()?.team
            if (team != null && team.id > 0) {
                TeamFixturesActivity.startActivity(
                    context = requireContext(),
                    teamId = team.id,
                    teamName = team.name,
                    teamLogo = team.logo
                )
            }
        }

        // Top Assists Adapter
        topAssistsAdapter = TopStatsAdapter(isAssists = true) { playerItem ->
            val team = playerItem.statistics.firstOrNull()?.team
            if (team != null && team.id > 0) {
                TeamFixturesActivity.startActivity(
                    context = requireContext(),
                    teamId = team.id,
                    teamName = team.name,
                    teamLogo = team.logo
                )
            }
        }

        wcGroupsAdapter = WcGroupsAdapter()

        binding.rvLeaguesContent.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        // Tab selection buttons (0: Standings, 1: Matches, 2: Top Scorers, 3: Top Assists)
        binding.btnStandings.setOnClickListener { viewModel.selectTab(0) }
        binding.btnMatches.setOnClickListener { viewModel.selectTab(1) }
        binding.btnScorers.setOnClickListener { viewModel.selectTab(2) }
        binding.btnAssists.setOnClickListener { viewModel.selectTab(3) }

        binding.rvLeaguesContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.selectedTab.value == 1) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 5) {
                        viewModel.loadMoreMatches()
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // Observe Leagues list
                launch {
                    viewModel.leagues.collect { list ->
                        leagueSelectorAdapter.submitList(list)
                    }
                }

                // Observe Selected League for UI Highlights
                launch {
                    viewModel.selectedLeagueId.collect { leagueId ->
                        leagueSelectorAdapter.setSelectedLeagueId(leagueId)
                        updateTabButtonsUI(viewModel.selectedTab.value)
                    }
                }

                // Observe Selected Tab for Adapter Swapping
                launch {
                    viewModel.selectedTab.collect { tabIndex ->
                        updateTabButtonsUI(tabIndex)
                    }
                }

                // Observe Standings State
                launch {
                    viewModel.standingsState.collect { state ->
                        if (viewModel.selectedTab.value == 0) {
                            renderStandingsState(state)
                        }
                    }
                }

                // Observe League Matches State
                launch {
                    viewModel.matchesState.collect { state ->
                        if (viewModel.selectedTab.value == 1) {
                            renderMatchesState(state)
                        }
                    }
                }

                // Observe Top Scorers State
                launch {
                    viewModel.topScorersState.collect { state ->
                        if (viewModel.selectedTab.value == 2) {
                            renderTopPlayersState(state, isAssists = false)
                        }
                    }
                }

                // Observe Top Assists State
                launch {
                    viewModel.topAssistsState.collect { state ->
                        if (viewModel.selectedTab.value == 3) {
                            renderTopPlayersState(state, isAssists = true)
                        }
                    }
                }

                // Observe refreshing state
                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshLayout.isRefreshing = refreshing
                    }
                }
            }
        }
    }

    private fun updateTabButtonsUI(selectedTab: Int) {
        val ctx = requireContext()
        val buttons = listOf(binding.btnStandings, binding.btnMatches, binding.btnScorers, binding.btnAssists)
        buttons.forEach {
            it.strokeWidth = 0
            it.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }

        val activeBtn = when (selectedTab) {
            0 -> binding.btnStandings
            1 -> binding.btnMatches
            2 -> binding.btnScorers
            3 -> binding.btnAssists
            else -> binding.btnStandings
        }

        activeBtn.apply {
            strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
            strokeWidth = dpToPx(1f)
            setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
        }

        when (selectedTab) {
            0 -> {
                val isWc = viewModel.selectedLeagueId.value == 1
                binding.layoutTableHeader.isVisible = !isWc
                binding.rvLeaguesContent.adapter = if (isWc) wcGroupsAdapter else standingsAdapter
                
                // Re-render standing state
                renderStandingsState(viewModel.standingsState.value)
            }
            1 -> {
                binding.layoutTableHeader.isVisible = false
                binding.rvLeaguesContent.adapter = leagueMatchesAdapter
                
                // Re-render matches state
                renderMatchesState(viewModel.matchesState.value)
            }
            2 -> {
                binding.layoutTableHeader.isVisible = false
                binding.rvLeaguesContent.adapter = topScorersAdapter
                
                // Re-render scorers state
                renderTopPlayersState(viewModel.topScorersState.value, isAssists = false)
            }
            3 -> {
                binding.layoutTableHeader.isVisible = false
                binding.rvLeaguesContent.adapter = topAssistsAdapter
                
                // Re-render assists state
                renderTopPlayersState(viewModel.topAssistsState.value, isAssists = true)
            }
        }
    }

    private fun renderStandingsState(state: StandingsUiState) {
        val isWc = viewModel.selectedLeagueId.value == 1
        when (state) {
            is StandingsUiState.Loading -> {
                binding.emptyState.isVisible = false
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.loadingSpinner.isVisible = true
                    if (isWc) {
                        wcGroupsAdapter.submitList(emptyList())
                    } else {
                        standingsAdapter.submitList(emptyList())
                    }
                }
            }
            is StandingsUiState.Error -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyState.text = getString(R.string.empty_fixtures)
                if (isWc) {
                    wcGroupsAdapter.submitList(emptyList())
                } else {
                    standingsAdapter.submitList(emptyList())
                }
            }
            is StandingsUiState.Success -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = state.list.isEmpty()
                if (state.list.isEmpty()) {
                    binding.emptyState.text = getString(R.string.empty_fixtures)
                }
                if (isWc) {
                    val grouped = state.list.groupBy { it.group ?: "Group A" }
                    val sortedGroups = grouped.entries.sortedBy { it.key }.map {
                        WcGroupItem(it.key, it.value)
                    }
                    wcGroupsAdapter.submitList(sortedGroups)
                } else {
                    standingsAdapter.submitList(state.list)
                }
            }
        }
    }

    private fun renderMatchesState(state: LeagueMatchesUiState) {
        when (state) {
            is LeagueMatchesUiState.Loading -> {
                binding.emptyState.isVisible = false
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.loadingSpinner.isVisible = true
                    leagueMatchesAdapter.submitList(emptyList())
                }
            }
            is LeagueMatchesUiState.Error -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyState.text = getString(R.string.empty_fixtures)
                leagueMatchesAdapter.submitList(emptyList())
            }
            is LeagueMatchesUiState.Success -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = state.items.isEmpty()
                if (state.items.isEmpty()) {
                    binding.emptyState.text = getString(R.string.empty_fixtures)
                }
                leagueMatchesAdapter.submitList(state.items) {
                    if (state.scrollToPosition in state.items.indices) {
                        (binding.rvLeaguesContent.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(state.scrollToPosition, 0)
                    }
                }
            }
        }
    }

    private fun renderTopPlayersState(state: TopPlayersUiState, isAssists: Boolean) {
        val adapter = if (isAssists) topAssistsAdapter else topScorersAdapter
        when (state) {
            is TopPlayersUiState.Loading -> {
                binding.emptyState.isVisible = false
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.loadingSpinner.isVisible = true
                    adapter.submitList(emptyList())
                }
            }
            is TopPlayersUiState.Error -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyState.text = getString(R.string.empty_fixtures)
                adapter.submitList(emptyList())
            }
            is TopPlayersUiState.Success -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = state.list.isEmpty()
                if (state.list.isEmpty()) {
                    binding.emptyState.text = getString(R.string.empty_fixtures)
                }
                adapter.submitList(state.list)
            }
        }
    }

    private fun loadNativeAd() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding.frAdsLeague.visibility = View.GONE
            return
        }

        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_all", getString(R.string.native_all))
        } catch (e: Exception) {
            getString(R.string.native_all)
        }
        if (adId.isNotEmpty()) {
            binding.frAdsLeague.visibility = View.VISIBLE
            // Inflate and show shimmer layout while loading
            val shimmerView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_shimmer_league, binding.frAdsLeague, false)
            binding.frAdsLeague.removeAllViews()
            binding.frAdsLeague.addView(shimmerView)

            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                liveScoreApiService, requireContext(), "native", adId, "Leagues"
            )

            Admob.getInstance().loadNativeAds(
                requireContext(),
                adId,
                1,
                object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        if (!isAdded) return

                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                            liveScoreApiService, requireContext(), "native", adId, "Leagues"
                        )

                        nativeAd?.setOnPaidEventListener { adValue ->
                            val ecpm = adValue.valueMicros / 1000.0
                            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                                liveScoreApiService, requireContext(), "native", adId, "Leagues", ecpm
                            )
                        }

                        val adView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.layout_native_league, null) as NativeAdView
                        
                        binding.frAdsLeague.removeAllViews()
                        binding.frAdsLeague.addView(adView)
                        
                        Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    }

                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        if (!isAdded) return
                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                            liveScoreApiService, requireContext(), "native", adId, "Leagues", null
                        )
                        binding.frAdsLeague.visibility = View.GONE
                    }
                }
            )
        } else {
            binding.frAdsLeague.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
