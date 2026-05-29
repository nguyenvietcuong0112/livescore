package com.livescore.football.livescores.footballscores.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.livescore.football.livescores.footballscores.base.BaseActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.databinding.ActivityMainBinding
import com.livescore.football.livescores.footballscores.ui.custom.PremiumPaywallDialog
import com.livescore.football.livescores.footballscores.ui.favorite.FavoriteFragment
import com.livescore.football.livescores.footballscores.ui.home.HomeFragment
import com.livescore.football.livescores.footballscores.ui.leagues.LeaguesFragment
import com.livescore.football.livescores.footballscores.ui.onboarding.IAPActivity
import com.livescore.football.livescores.footballscores.ui.profile.ProfileFragment
import com.livescore.football.livescores.footballscores.ui.search.SearchActivity
import com.livescore.football.livescores.footballscores.ui.wc26.WC26Fragment
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityMainBinding
    private var isLimitDialogShowing = false
    private var activeTabId: Int = R.id.nav_live

    private val handlerADS = Handler(Looper.getMainLooper())
    private var isFirstLoad = true
    private var delayedLoadExpandTask: Runnable? = null
    private var savedState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        savedState = savedInstanceState
        super.onCreate(savedInstanceState)
    }

    override fun bind() {
        // Enable edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Adjust toolbar top margin to prevent overlapping with status bar icons
            val toolbarParams = binding.toolbar.layoutParams as android.view.ViewGroup.MarginLayoutParams
            toolbarParams.topMargin = systemBars.top
            binding.toolbar.layoutParams = toolbarParams
            
            // Adjust bottom navigation and banner ads padding to fit navigation bar
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            binding.frAdsBanner.setPadding(0, 0, 0, systemBars.bottom)
            
            insets
        }

        binding.frAdsBanner.bringToFront()
        binding.frAdsCollap.bringToFront()
        binding.btnFloatingWc.bringToFront()

        binding.searchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.btnFloatingWc.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_wc26
        }

        if (savedState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.Companion.newInstance(false))
                .commit()
            updateLogoText(R.id.nav_live)
        } else {
            activeTabId = binding.bottomNavigation.selectedItemId
            updateLogoText(activeTabId)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (activeTabId == item.itemId) {
                return@setOnItemSelectedListener false
            }

            activeTabId = item.itemId
            updateLogoText(item.itemId)
            val fragment = when (item.itemId) {
                R.id.nav_live -> HomeFragment.Companion.newInstance(false)
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
                launch {
                    limitManager.limitExceededFlow.collect {
                        if (!limitManager.isPremium()) {
                            showPremiumPaywall(isOutOfQuota = true)
                        }
                    }
                }
                launch {
                    var wasPremium = limitManager.isPremium()
                    limitManager.isPremiumFlow.collect { isPremium ->
                        if (isPremium && !wasPremium) {
                            recreate()
                        }
                        wasPremium = isPremium
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
        val existing = supportFragmentManager.findFragmentByTag(PremiumPaywallDialog.Companion.TAG)
        if (existing == null) {
            val paywall = PremiumPaywallDialog.Companion.newInstance(isOutOfQuota)
            paywall.show(supportFragmentManager, PremiumPaywallDialog.Companion.TAG)
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
            RemoteConfigManager.Companion.getInstance()
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
                binding.bottomNavigation.visibility = View.GONE
                binding.btnFloatingWc.visibility = View.GONE

                binding.frAdsCollap.removeAllViews()

                val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                val closeButton = adView.findViewById<ImageView>(R.id.close)

                closeButton?.setOnClickListener {
                    binding.frAdsCollap.removeAllViews()
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.btnFloatingWc.visibility = View.VISIBLE
                    binding.btnFloatingWc.bringToFront()
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
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.btnFloatingWc.visibility = View.VISIBLE
                binding.btnFloatingWc.bringToFront()
            }
        })
    }

    private fun loadNativeBanner(onLoaded: (() -> Unit)? = null) {
        binding.frAdsCollap.removeAllViews()

        val nativeAllId = try {
            RemoteConfigManager.Companion.getInstance()
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
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.btnFloatingWc.visibility = View.VISIBLE
                binding.btnFloatingWc.bringToFront()

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
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.btnFloatingWc.visibility = View.VISIBLE
                binding.btnFloatingWc.bringToFront()

                onLoaded?.invoke()
            }
        })
    }

    private fun updateVipButtonVisibility() {
        if (limitManager.isPremium()) {
            binding.btnGoVip.visibility = View.GONE
        } else {
            binding.btnGoVip.visibility = View.VISIBLE
            binding.btnGoVip.setOnClickListener {
                startActivity(Intent(this, IAPActivity::class.java))
            }
        }
    }

    private fun updateLogoText(tabId: Int) {
        if (tabId == R.id.nav_live) {
            binding.logoImage.visibility = View.VISIBLE
            binding.logoText.visibility = View.GONE
        } else {
            binding.logoImage.visibility = View.GONE
            binding.logoText.visibility = View.VISIBLE
            
            val title = when (tabId) {
                R.id.nav_leagues -> getString(R.string.leagues_header_title)
                R.id.nav_wc26 -> getString(R.string.splash_app_title)
                R.id.nav_favorite -> getString(R.string.favorite_title)
                R.id.nav_profile -> getString(R.string.profile_settings)
                else -> getString(R.string.splash_app_title)
            }
            binding.logoText.text = title
        }
    }
}