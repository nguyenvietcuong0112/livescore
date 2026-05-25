package com.livescore.football.livescores.footballscores.ui.wc26

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.repository.LeaguesRepository
import com.livescore.football.livescores.footballscores.databinding.FragmentWc26Binding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class WC26Fragment : Fragment() {

    private var _binding: FragmentWc26Binding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var leaguesRepository: LeaguesRepository

    private var selectedTab = 0 // 0: Fixtures, 1: Groups, 2: Teams, 3: News

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWc26Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCountdown()
        setupListeners()
        updateTabUI()
        loadWcStandings()
    }

    private fun loadWcStandings() {
        lifecycleScope.launch {
            // League ID 1 represents World Cup, season 2026
            leaguesRepository.getStandings(1, 2026)
                .catch { e ->
                    e.printStackTrace()
                    showErrorState()
                }
                .collect { list ->
                    if (list.size >= 8) {
                        // Dynamically populate Group A
                        binding.tvGroupATeam1.text = "1. ${list[0].team.name}"
                        binding.tvGroupATeam2.text = "2. ${list[1].team.name}"
                        binding.tvGroupATeam3.text = "3. ${list[2].team.name}"
                        binding.tvGroupATeam4.text = "4. ${list[3].team.name}"

                        // Dynamically populate Group B
                        binding.tvGroupBTeam1.text = "1. ${list[4].team.name}"
                        binding.tvGroupBTeam2.text = "2. ${list[5].team.name}"
                        binding.tvGroupBTeam3.text = "3. ${list[6].team.name}"
                        binding.tvGroupBTeam4.text = "4. ${list[7].team.name}"
                    } else {
                        showErrorState()
                    }
                }
        }
    }

    private fun showErrorState() {
        binding.tvGroupATeam1.text = "1. Data N/A"
        binding.tvGroupATeam2.text = "2. Data N/A"
        binding.tvGroupATeam3.text = "3. Data N/A"
        binding.tvGroupATeam4.text = "4. Data N/A"

        binding.tvGroupBTeam1.text = "1. Data N/A"
        binding.tvGroupBTeam2.text = "2. Data N/A"
        binding.tvGroupBTeam3.text = "3. Data N/A"
        binding.tvGroupBTeam4.text = "4. Data N/A"
    }

    private fun setupCountdown() {
        // Calculate remaining days until World Cup kickoff: June 11, 2026
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 11)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffMs = targetCal.timeInMillis - System.currentTimeMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
        
        val countdownText = if (diffDays > 0) {
            getString(R.string.wc26_days_until, diffDays)
        } else if (diffDays == 0L) {
            getString(R.string.wc26_starts_today)
        } else {
            getString(R.string.wc26_ongoing)
        }
        binding.tvCountdown.text = countdownText
    }

    private fun setupListeners() {
        binding.tabWcFixtures.setOnClickListener {
            selectedTab = 0
            updateTabUI()
        }
        binding.tabWcGroups.setOnClickListener {
            selectedTab = 1
            updateTabUI()
        }
        binding.tabWcTeams.setOnClickListener {
            selectedTab = 2
            updateTabUI()
        }
        binding.tabWcNews.setOnClickListener {
            selectedTab = 3
            updateTabUI()
        }

        // Click reference news card to route to Main Leagues tab
        binding.cardLinkToGeneralNews.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_leagues)
        }
    }

    private fun updateTabUI() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        
        // Reset all buttons
        binding.tabWcFixtures.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }
        binding.tabWcGroups.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }
        binding.tabWcTeams.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }
        binding.tabWcNews.apply {
            strokeWidth = 0
            setTextColor(inactiveColor)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
        }

        // Hide all views
        binding.scrollWcFixtures.isVisible = false
        binding.scrollWcGroups.isVisible = false
        binding.scrollWcTeams.isVisible = false
        binding.scrollWcNews.isVisible = false

        // Apply active button styles and show active views
        when (selectedTab) {
            0 -> {
                binding.tabWcFixtures.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcFixtures.isVisible = true
            }
            1 -> {
                binding.tabWcGroups.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcGroups.isVisible = true
            }
            2 -> {
                binding.tabWcTeams.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcTeams.isVisible = true
            }
            3 -> {
                binding.tabWcNews.apply {
                    strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
                    strokeWidth = dpToPx(1.5f)
                    setTextColor(activeColor)
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.card_dark)
                }
                binding.scrollWcNews.isVisible = true
            }
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
