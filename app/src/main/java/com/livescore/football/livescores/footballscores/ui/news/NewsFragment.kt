package com.livescore.football.livescores.footballscores.ui.news

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
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.FragmentNewsBinding
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewsFragment : Fragment() {

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var categoryAdapter: NewsCategoryAdapter

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(
            requireContext().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "News"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoriesRecyclerView()
        setupNewsRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupCategoriesRecyclerView() {
        categoryAdapter = NewsCategoryAdapter { category ->
            viewModel.selectCategory(category.id)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupNewsRecyclerView() {
        newsAdapter = NewsAdapter { newsItem ->
            val act = activity as? androidx.appcompat.app.AppCompatActivity
            if (act != null) {
                AdsConfig.showInterClickAd(act) {
                    NewsDetailActivity.startActivity(requireContext(), newsItem)
                }
            } else {
                NewsDetailActivity.startActivity(requireContext(), newsItem)
            }
        }
        binding.rvNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadData()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = false
                    when (state) {
                        is NewsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvNews.isVisible = false
                            binding.layoutError.isVisible = false
                        }
                        is NewsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.rvNews.isVisible = true

                            categoryAdapter.selectedCategoryId = state.selectedCategoryId
                            categoryAdapter.submitList(state.categories)

                            newsAdapter.submitList(state.filteredNews) {
                                binding.rvNews.scrollToPosition(0)
                            }

                            if (state.filteredNews.isEmpty()) {
                                binding.layoutError.isVisible = true
                                binding.tvErrorMessage.text =
                                    getString(R.string.news_no_data_category)
                            }
                        }
                        is NewsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.rvNews.isVisible = false
                            binding.layoutError.isVisible = true
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }
}
