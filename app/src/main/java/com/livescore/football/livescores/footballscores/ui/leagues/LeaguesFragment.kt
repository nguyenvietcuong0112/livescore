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
import com.bumptech.glide.Glide
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.FragmentLeaguesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import javax.inject.Inject

@AndroidEntryPoint
class LeaguesFragment : Fragment() {

    @Inject
    lateinit var limitManager: com.livescore.football.livescores.footballscores.data.local.RequestLimitManager

    private var _binding: FragmentLeaguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaguesViewModel by viewModels()

    private lateinit var leagueSelectorAdapter: LeagueSelectorAdapter
    private lateinit var standingsAdapter: StandingsAdapter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        loadNativeAd()
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
            // Handle team click if needed, or simple feedback
        }

        // Top Scorers Adapter
        topScorersAdapter = TopStatsAdapter(isAssists = false) { playerItem ->
            // Handle player click if needed
        }

        // Top Assists Adapter
        topAssistsAdapter = TopStatsAdapter(isAssists = true) { playerItem ->
            // Handle player click if needed
        }

        binding.rvLeaguesContent.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        // Tab selection buttons
        binding.btnStandings.setOnClickListener { viewModel.selectTab(0) }
        binding.btnScorers.setOnClickListener { viewModel.selectTab(1) }
        binding.btnAssists.setOnClickListener { viewModel.selectTab(2) }
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

                // Observe Top Scorers State
                launch {
                    viewModel.topScorersState.collect { state ->
                        if (viewModel.selectedTab.value == 1) {
                            renderTopPlayersState(state, isAssists = false)
                        }
                    }
                }

                // Observe Top Assists State
                launch {
                    viewModel.topAssistsState.collect { state ->
                        if (viewModel.selectedTab.value == 2) {
                            renderTopPlayersState(state, isAssists = true)
                        }
                    }
                }
            }
        }
    }

    private fun updateTabButtonsUI(selectedTab: Int) {
        val ctx = requireContext()
        val buttons = listOf(binding.btnStandings, binding.btnScorers, binding.btnAssists)
        buttons.forEach { it.strokeWidth = 0 }

        when (selectedTab) {
            0 -> {
                binding.btnStandings.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = dpToPx(1f)
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnScorers.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnAssists.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                
                binding.layoutTableHeader.isVisible = true
                binding.rvLeaguesContent.adapter = standingsAdapter
                
                // Re-render standing state
                renderStandingsState(viewModel.standingsState.value)
            }
            1 -> {
                binding.btnScorers.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = dpToPx(1f)
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnStandings.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnAssists.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                
                binding.layoutTableHeader.isVisible = false
                binding.rvLeaguesContent.adapter = topScorersAdapter
                
                // Re-render scorers state
                renderTopPlayersState(viewModel.topScorersState.value, isAssists = false)
            }
            2 -> {
                binding.btnAssists.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = dpToPx(1f)
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnStandings.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnScorers.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                
                binding.layoutTableHeader.isVisible = false
                binding.rvLeaguesContent.adapter = topAssistsAdapter
                
                // Re-render assists state
                renderTopPlayersState(viewModel.topAssistsState.value, isAssists = true)
            }
        }
    }

    private fun renderStandingsState(state: StandingsUiState) {
        when (state) {
            is StandingsUiState.Loading -> {
                binding.loadingSpinner.isVisible = true
                binding.emptyState.isVisible = false
                standingsAdapter.submitList(emptyList())
            }
            is StandingsUiState.Error -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyState.text = getString(R.string.empty_fixtures)
                standingsAdapter.submitList(emptyList())
            }
            is StandingsUiState.Success -> {
                binding.loadingSpinner.isVisible = false
                binding.emptyState.isVisible = state.list.isEmpty()
                if (state.list.isEmpty()) {
                    binding.emptyState.text = getString(R.string.empty_fixtures)
                }
                standingsAdapter.submitList(state.list)
            }
        }
    }

    private fun renderTopPlayersState(state: TopPlayersUiState, isAssists: Boolean) {
        val adapter = if (isAssists) topAssistsAdapter else topScorersAdapter
        when (state) {
            is TopPlayersUiState.Loading -> {
                binding.loadingSpinner.isVisible = true
                binding.emptyState.isVisible = false
                adapter.submitList(emptyList())
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
            Admob.getInstance().loadNativeAds(
                requireContext(),
                adId,
                1,
                object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        if (!isAdded) return
                        val adView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.layout_native_league, null) as NativeAdView
                        
                        binding.frAdsLeague.removeAllViews()
                        binding.frAdsLeague.addView(adView)
                        
                        Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    }

                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        if (!isAdded) return
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
