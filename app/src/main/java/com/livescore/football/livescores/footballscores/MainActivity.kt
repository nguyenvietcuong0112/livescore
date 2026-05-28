package com.livescore.football.livescores.footballscores

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.ActivityMainBinding
import com.livescore.football.livescores.footballscores.ui.favorite.FavoriteFragment
import com.livescore.football.livescores.footballscores.ui.home.HomeFragment
import com.livescore.football.livescores.footballscores.ui.leagues.LeaguesFragment
import com.livescore.football.livescores.footballscores.ui.wc26.WC26Fragment
import com.livescore.football.livescores.footballscores.ui.profile.ProfileFragment
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallBottomSheet
import com.livescore.football.livescores.footballscores.ui.onboarding.IAPActivity
import com.livescore.football.livescores.footballscores.ui.search.SearchActivity
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityMainBinding
    private var isLimitDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar action
        binding.searchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Setup custom floating World Cup gold button click
        binding.btnFloatingWc.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_wc26
        }

        // Set default fragment (Live Match Center)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.newInstance(false))
                .commit()
        }

        // Setup bottom navigation selection with Interstitial Ad frequency capping (35s)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentSelectedId = binding.bottomNavigation.selectedItemId
            if (currentSelectedId == item.itemId) {
                return@setOnItemSelectedListener false
            }

            AdsConfig.showInterClickAd(this) {
                val fragment = when (item.itemId) {
                    R.id.nav_live -> HomeFragment.newInstance(false)
                    R.id.nav_leagues -> LeaguesFragment()
                    R.id.nav_wc26 -> WC26Fragment()
                    R.id.nav_favorite -> FavoriteFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> null
                }
                if (fragment != null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
            }
            true
        }

        // Observe request limit exceeds safely in RESUMED state to prevent StateLoss crashes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                limitManager.limitExceededFlow.collect {
                    if (!limitManager.isPremium()) {
                        showPremiumPaywall()
                    }
                }
            }
        }
    }

    fun switchToTab(tabId: Int) {
        binding.bottomNavigation.selectedItemId = tabId
    }

    fun showPremiumPaywall() {
        if (supportFragmentManager.isStateSaved) return
        val existing = supportFragmentManager.findFragmentByTag(PremiumPaywallBottomSheet.TAG)
        if (existing == null) {
            val paywall = PremiumPaywallBottomSheet.newInstance()
            paywall.show(supportFragmentManager, PremiumPaywallBottomSheet.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::limitManager.isInitialized) {
            updateVipButtonVisibility()
        }
    }

    private fun updateVipButtonVisibility() {
        if (limitManager.isPremium()) {
            binding.btnGoVip.visibility = android.view.View.GONE
        } else {
            binding.btnGoVip.visibility = android.view.View.VISIBLE
            binding.btnGoVip.setOnClickListener {
                startActivity(Intent(this, IAPActivity::class.java))
            }
        }
    }
}
