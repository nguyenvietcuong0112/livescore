package com.livescore.football.livescores.footballscores.ui.favorite

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
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.databinding.FragmentFavoriteBinding
import com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity
import com.livescore.football.livescores.footballscores.ui.home.MatchAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import com.livescore.football.livescores.footballscores.data.local.MatchReminderManager
import javax.inject.Inject
import androidx.core.content.ContextCompat
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.ui.home.MatchListItem
import com.livescore.football.livescores.footballscores.utils.bindScrollableChild

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter

    @Inject
    lateinit var reminderManager: MatchReminderManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private var pendingReminderAction: (() -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingReminderAction?.invoke()
        }
        pendingReminderAction = null
    }

    private fun showSettingsDialog() {
        val dialog = com.livescore.football.livescores.footballscores.ui.custom.NotificationPermissionDialog.newInstance()
        dialog.show(childFragmentManager, com.livescore.football.livescores.footballscores.ui.custom.NotificationPermissionDialog.TAG)
    }


    private fun checkAndRequestNotificationPermission(onGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                val sharedPrefs = requireContext().getSharedPreferences("livescore_permissions_prefs", android.content.Context.MODE_PRIVATE)
                val hasRequestedBefore = sharedPrefs.getBoolean("has_requested_notification", false)
                val showRationale = shouldShowRequestPermissionRationale(permission)

                if (hasRequestedBefore && !showRationale) {
                    showSettingsDialog()
                } else {
                    pendingReminderAction = onGranted
                    sharedPrefs.edit().putBoolean("has_requested_notification", true).apply()
                    notificationPermissionLauncher.launch(permission)
                }
            }
        } else {
            onGranted()
        }
    }

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

        binding.swipeRefreshLayout.apply {
            bindScrollableChild { binding.rvMatches }
            setOnRefreshListener { viewModel.refreshFavoriteMatchesFromServer() }
        }

        binding.btnDiscover.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_live)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite matches when fragment is shown
        viewModel.loadFavoriteMatches()
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Favorite"
        )
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
            },
            onReminderClick = { match ->
                checkAndRequestNotificationPermission {
                    val isSet = reminderManager.toggleReminder(match)
                    val alertMsg = if (isSet) {
                        getString(R.string.reminder_set_toast)
                    } else {
                        getString(R.string.reminder_cancelled_toast)
                    }
                    android.widget.Toast.makeText(requireContext(), alertMsg, android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.loadFavoriteMatches() // Refresh visual state
                }
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
                        binding.tvEmptyMessage.text = getString(R.string.no_favorite_matches)
                        
                        val withAds = mutableListOf<MatchListItem>()
                        var matchCount = 0
                        items.forEach { item ->
                            withAds.add(item)
                            if (item is MatchListItem.MatchItem) {
                                matchCount++
                                if (!limitManager.isPremium() && matchCount % 3 == 0) {
                                    withAds.add(MatchListItem.NativeAd(id = "fav_ad_$matchCount"))
                                }
                            }
                        }
                        matchAdapter.submitList(withAds)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
