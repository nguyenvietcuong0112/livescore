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
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog
import com.livescore.football.livescores.footballscores.ui.onboarding.IAPActivity
import android.view.LayoutInflater
import android.widget.ImageView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.ui.search.SearchActivity
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityMainBinding
    private var isLimitDialogShowing = false
    private var activeTabId: Int = R.id.nav_live

    private val handlerADS = android.os.Handler(android.os.Looper.getMainLooper())
    private var isFirstLoad = true
    private var delayedLoadExpandTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force ad layouts to draw on top of the bottom navigation bar and floating gold button
        binding.frAdsBanner.bringToFront()
        binding.frAdsCollap.bringToFront()

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
            if (activeTabId == item.itemId) {
                return@setOnItemSelectedListener false
            }

            activeTabId = item.itemId
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
                    .commitAllowingStateLoss()
            }

            AdsConfig.showInterClickAd(this) {
                // Done in background without blocking transitions
            }
            true
        }

        // Observe request limit exceeds safely in RESUMED state to prevent StateLoss crashes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                limitManager.limitExceededFlow.collect {
                    if (!limitManager.isPremium()) {
                        showPremiumPaywall(isOutOfQuota = true)
                    }
                }
            }
        }
    }

    fun switchToTab(tabId: Int) {
        binding.bottomNavigation.selectedItemId = tabId
    }

    fun showPremiumPaywall(isOutOfQuota: Boolean = false) {
        if (supportFragmentManager.isStateSaved) return
        val existing = supportFragmentManager.findFragmentByTag(PremiumPaywallDialog.TAG)
        if (existing == null) {
            val paywall = PremiumPaywallDialog.newInstance(isOutOfQuota)
            paywall.show(supportFragmentManager, PremiumPaywallDialog.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::limitManager.isInitialized) {
            updateVipButtonVisibility()
        }

        if (!limitManager.isPremium()) {
            if (isFirstLoad) {
                loadNativeBanner {
                    delayedLoadExpandTask = Runnable {
                        loadNativeBannerse()
                        isFirstLoad = false
                    }
                    handlerADS.postDelayed(delayedLoadExpandTask!!, 1000)
                }
            } else {
                loadNativeBanner {
                    delayedLoadExpandTask = Runnable {
                        loadNativeBannerse()
                    }
                    handlerADS.postDelayed(delayedLoadExpandTask!!, 35000)
                }
            }
        } else {
            binding.frAdsCollap.removeAllViews()
            binding.frAdsBanner.removeAllViews()
        }
    }

    override fun onPause() {
        super.onPause()
        delayedLoadExpandTask?.let {
            handlerADS.removeCallbacks(it)
            delayedLoadExpandTask = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerADS.removeCallbacksAndMessages(null)
    }

    private fun loadNativeBannerse() {
        val nativeAllId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_all", getString(R.string.native_all))
        } catch (e: Exception) {
            getString(R.string.native_all)
        }

        Admob.getInstance().loadNativeAd(this, nativeAllId, object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                if (isDestroyed || isFinishing) return
                val adView = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.layout_native_home_collapse, null) as NativeAdView

                // Hide bottom navigation and floating trophy button so they never overlap the expanded ad view
                binding.bottomNavigation.visibility = android.view.View.GONE
                binding.btnFloatingWc.visibility = android.view.View.GONE

                binding.frAdsCollap.removeAllViews()

                val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                val closeButton = adView.findViewById<ImageView>(R.id.close)

                closeButton?.setOnClickListener {
                    binding.frAdsCollap.removeAllViews()
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                    binding.btnFloatingWc.visibility = android.view.View.VISIBLE
                    loadNativeBanner()
                }

                binding.frAdsCollap.addView(adView)
                binding.frAdsCollap.bringToFront() // Force draw on top of navbar
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
            }

            override fun onAdFailedToLoad() {
                if (isDestroyed || isFinishing) return
                binding.frAdsCollap.removeAllViews()
                
                // Show bottom navigation and floating button again if ad fails to load
                binding.bottomNavigation.visibility = android.view.View.VISIBLE
                binding.btnFloatingWc.visibility = android.view.View.VISIBLE
            }
        })
    }

    private fun loadNativeBanner(onLoaded: (() -> Unit)? = null) {
        binding.frAdsCollap.removeAllViews()

        val nativeAllId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_all", getString(R.string.native_all))
        } catch (e: Exception) {
            getString(R.string.native_all)
        }

        Admob.getInstance().loadNativeAd(this, nativeAllId, object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                if (isDestroyed || isFinishing) return
                val adView = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.layout_native_banner, null) as NativeAdView

                // Show bottom navigation and floating button when collapsed ad is displayed
                binding.bottomNavigation.visibility = android.view.View.VISIBLE
                binding.btnFloatingWc.visibility = android.view.View.VISIBLE

                binding.frAdsBanner.removeAllViews()
                binding.frAdsBanner.addView(adView)
                binding.frAdsBanner.bringToFront() // Force draw on top of navbar

                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                onLoaded?.invoke()
            }

            override fun onAdFailedToLoad() {
                if (isDestroyed || isFinishing) return
                binding.frAdsBanner.removeAllViews()
                
                // Ensure navbar is visible if ad failed
                binding.bottomNavigation.visibility = android.view.View.VISIBLE
                binding.btnFloatingWc.visibility = android.view.View.VISIBLE
                
                onLoaded?.invoke()
            }
        })
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
