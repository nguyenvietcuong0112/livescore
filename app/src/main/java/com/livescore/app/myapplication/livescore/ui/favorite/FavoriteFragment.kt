package com.livescore.app.myapplication.livescore.ui.favorite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.databinding.FragmentFavoriteBinding
import com.livescore.app.myapplication.livescore.ui.detail.MatchDetailActivity
import com.livescore.app.myapplication.livescore.ui.home.MatchAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite matches when fragment is shown
        viewModel.loadFavoriteMatches()
    }

    private fun setupRecyclerViews() {
        // Matches Adapter
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
                viewModel.toggleFixtureFavorite(match)
            }
        )
        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatches.adapter = matchAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe favorite matches
                launch {
                    viewModel.favoriteMatches.collect { items ->
                        binding.emptyStateLayout.isVisible = items.isEmpty()
                        binding.tvEmptyMessage.text = "Chưa có trận đấu yêu thích nào"
                        matchAdapter.submitList(items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
