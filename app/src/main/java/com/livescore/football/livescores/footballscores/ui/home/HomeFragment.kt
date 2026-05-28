package com.livescore.football.livescores.footballscores.ui.home

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.FavoriteManager
import com.livescore.football.livescores.footballscores.data.local.MatchReminderManager
import com.livescore.football.livescores.footballscores.databinding.FragmentHomeBinding
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog
import com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var dateAdapter: DateAdapter

    @Inject
    lateinit var reminderManager: MatchReminderManager

    @Inject
    lateinit var favoriteManager: FavoriteManager



    private var pendingReminderAction: (() -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingReminderAction?.invoke()
        }
        pendingReminderAction = null
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
                pendingReminderAction = onGranted
                notificationPermissionLauncher.launch(permission)
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLiveOnly = arguments?.getBoolean(ARG_LIVE_ONLY, false) ?: false
        if (isLiveOnly) {
            binding.timeFilterLayout.visibility = View.GONE
            binding.filterLayout.visibility = View.GONE
            binding.emptyState.text = getString(R.string.empty_fixtures)
            viewModel.setFilter(MatchFilter.LIVE)
        }

        setupRecyclerViews()
        setupFilters()
        setupCalendarPicker()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh reminder state when user navigates back to screen
        viewModel.refreshReminders()
        if (viewModel.currentFilter.value == MatchFilter.LIVE) {
            viewModel.startLivePolling()
        }
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
                if (favoriteManager.canAddFavoriteFixture(match.id)) {
                    viewModel.toggleFavorite(match)
                } else {
                    val paywall = PremiumPaywallDialog.newInstance()
                    paywall.show(parentFragmentManager, PremiumPaywallDialog.TAG)
                }
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
                    viewModel.refreshReminders()
                }
            }
        )
        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatches.adapter = matchAdapter

        // Date Selector Adapter setup
        dateAdapter = DateAdapter(
            onDateClick = { item, position ->
                viewModel.setSelectedDate(item.date)
            }
        )
        binding.rvDateSelector.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvDateSelector.adapter = dateAdapter
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

    private fun setupCalendarPicker() {
        binding.btnCalendar.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val selectedDateMs = viewModel.selectedDate.value.time
        val datePicker = MaterialDatePicker.Builder.datePicker().apply {
            setTheme(R.style.CustomDatePickerTheme)
            setTitleText(getString(R.string.select_match_date))
            setSelection(selectedDateMs)
        }.build()

        datePicker.addOnPositiveButtonClickListener { selectionTime ->
            // MaterialDatePicker uses UTC milliseconds. Map safely to local timezone to prevent shift bugs.
            val selectedCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectionTime
            }
            val localCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
            }
            viewModel.setSelectedDate(localCalendar.time)
        }
        datePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect Matches
                launch {
                    viewModel.matches.collect { items ->
                        binding.emptyState.isVisible = items.isEmpty()
                        matchAdapter.submitList(items) {
                            binding.rvMatches.scrollToPosition(0)
                        }
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

                // Collect Date selection and update Horizontal calendar strip
                launch {
                    viewModel.selectedDate.collect { date ->
                        dateAdapter.setSelectedDate(date)
                        binding.rvDateSelector.scrollToPosition(dateAdapter.getSelectedPosition())
                        
                        // Synchronize state: hide sub-filters layout when selected date is NOT today
                        val isLiveOnly = arguments?.getBoolean(ARG_LIVE_ONLY, false) ?: false
                        binding.filterLayout.isVisible = isToday(date) && !isLiveOnly
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

    override fun onPause() {
        super.onPause()
        viewModel.stopLivePolling()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
