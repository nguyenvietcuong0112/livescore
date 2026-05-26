package com.livescore.football.livescores.footballscores

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.ActivityMainBinding
import com.livescore.football.livescores.footballscores.ui.favorite.FavoriteFragment
import com.livescore.football.livescores.footballscores.ui.home.HomeFragment
import com.livescore.football.livescores.footballscores.ui.leagues.LeaguesFragment
import com.livescore.football.livescores.footballscores.ui.wc26.WC26Fragment
import com.livescore.football.livescores.footballscores.ui.profile.ProfileFragment
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallBottomSheet
import com.livescore.football.livescores.footballscores.ui.search.SearchActivity
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

        // Setup bottom navigation selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_live -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment.newInstance(false))
                        .commit()
                    true
                }
                R.id.nav_leagues -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LeaguesFragment())
                        .commit()
                    true
                }
                R.id.nav_wc26 -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, WC26Fragment())
                        .commit()
                    true
                }
                R.id.nav_favorite -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, FavoriteFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Observe request limit exceeds
        lifecycleScope.launch {
            limitManager.limitExceededFlow.collect {
                if (!limitManager.isPremium()) {
                    showPremiumPaywall()
                }
            }
        }
    }

    fun switchToTab(tabId: Int) {
        binding.bottomNavigation.selectedItemId = tabId
    }

    fun showPremiumPaywall() {
        val existing = supportFragmentManager.findFragmentByTag(PremiumPaywallBottomSheet.TAG)
        if (existing == null) {
            val paywall = PremiumPaywallBottomSheet.newInstance()
            paywall.show(supportFragmentManager, PremiumPaywallBottomSheet.TAG)
        }
    }
}
