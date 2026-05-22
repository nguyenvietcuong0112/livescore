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

        val isLiveOnly = arguments?.getBoolean(ARG_LIVE_ONLY, false) ?: false
        if (isLiveOnly) {
            binding.rvDates.visibility = View.GONE
            binding.dateDivider.visibility = View.GONE
            binding.filterLayout.visibility = View.GONE
            binding.emptyState.text = "No live matches currently in progress"
            viewModel.setFilter(MatchFilter.LIVE)
        }

        setupRecyclerViews()
        setupFilters()
        observeViewModel()
    }

    companion object {
        private const val ARG_LIVE_ONLY = "arg_live_only"

        fun newInstance(isLiveOnly: Boolean): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_LIVE_ONLY, isLiveOnly)
                }
            }
        }
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
            viewModel.setSelectedDate(dateItem.date)
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
                    viewModel.matches.collect { items ->
                        binding.emptyState.isVisible = items.isEmpty()
                        matchAdapter.submitList(items)
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
