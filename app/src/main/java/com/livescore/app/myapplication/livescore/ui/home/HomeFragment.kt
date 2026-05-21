package com.livescore.app.myapplication.livescore.ui.home

import android.content.Intent
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
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.databinding.FragmentHomeBinding
import com.livescore.app.myapplication.livescore.ui.detail.MatchDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var dateAdapter: DateAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupFilters()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Match Adapter setup
        matchAdapter = MatchAdapter(
            onMatchClick = { match ->
                val intent = Intent(requireContext(), MatchDetailActivity::class.java).apply {
                    putExtra("MATCH_ID", match.id)
                    putExtra("HOME_TEAM", match.homeTeamName)
                    putExtra("AWAY_TEAM", match.awayTeamName)
                }
                startActivity(intent)
            },
            onFavoriteClick = { match ->
                viewModel.toggleFavorite(match)
            }
        )
        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatches.adapter = matchAdapter

        // Date Adapter setup
        dateAdapter = DateAdapter { dateItem, position ->
            // Date filter mock action
        }
        binding.rvDates.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvDates.adapter = dateAdapter
    }

    private fun setupFilters() {
        binding.btnLive.setOnClickListener {
            viewModel.setFilter(MatchFilter.LIVE)
        }
        binding.btnScheduled.setOnClickListener {
            viewModel.setFilter(MatchFilter.UPCOMING)
        }
        binding.btnFinished.setOnClickListener {
            viewModel.setFilter(MatchFilter.FINISHED)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect Matches
                launch {
                    viewModel.matches.collect { matches ->
                        binding.emptyState.isVisible = matches.isEmpty()
                        val listItems = mapMatchesToListItems(matches)
                        matchAdapter.submitList(listItems)
                    }
                }

                // Collect Loading
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.loadingSpinner.isVisible = loading && matchAdapter.currentList.isEmpty()
                    }
                }

                // Collect Filter Tab Highlight updates
                launch {
                    viewModel.currentFilter.collect { filter ->
                        updateFilterButtonUI(filter)
                    }
                }
            }
        }
    }

    private fun mapMatchesToListItems(matches: List<CachedMatchEntity>): List<MatchListItem> {
        val items = mutableListOf<MatchListItem>()
        val grouped = matches.groupBy { it.leagueId }
        var matchCount = 0

        for ((leagueId, matchGroup) in grouped) {
            val firstMatch = matchGroup.first()
            items.add(MatchListItem.LeagueHeader(leagueId, firstMatch.leagueName, firstMatch.leagueLogo))
            for (match in matchGroup) {
                items.add(MatchListItem.MatchItem(match))
                matchCount++
                if (matchCount % 5 == 0) {
                    items.add(MatchListItem.NativeAd(
                        id = "ad_$matchCount",
                        title = "Unlock SofaScore Premium",
                        body = "Ad-free experience with real-time pressure pitch maps!"
                    ))
                }
            }
        }
        return items
    }

    private fun updateFilterButtonUI(selectedFilter: MatchFilter) {
        val ctx = requireContext()
        val buttons = listOf(binding.btnLive, binding.btnScheduled, binding.btnFinished)
        buttons.forEach { it.strokeWidth = 0 }

        when (selectedFilter) {
            MatchFilter.LIVE -> {
                binding.btnLive.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = 3
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnScheduled.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnFinished.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }
            MatchFilter.UPCOMING -> {
                binding.btnScheduled.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = 3
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnLive.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnFinished.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }
            MatchFilter.FINISHED -> {
                binding.btnFinished.apply {
                    strokeColor = ContextCompat.getColorStateList(ctx, R.color.accent_green)
                    strokeWidth = 3
                    setTextColor(ContextCompat.getColor(ctx, R.color.accent_green))
                }
                binding.btnLive.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                binding.btnScheduled.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.currentFilter.value == MatchFilter.LIVE) {
            viewModel.startLivePolling()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLivePolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
