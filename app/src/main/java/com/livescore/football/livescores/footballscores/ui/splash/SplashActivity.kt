package com.livescore.football.livescores.footballscores.ui.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.messaging.FirebaseMessaging
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.DeviceRegistrationManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.data.remote.adjust.RetentionTracker
import com.livescore.football.livescores.footballscores.data.remote.getRemoteAdId
import com.livescore.football.livescores.footballscores.databinding.ActivitySplashBinding
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV1
import com.livescore.football.livescores.footballscores.utils.SharePreferenceUtils
import com.livescore.football.livescores.footballscores.data.remote.PushClickTracker
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.livescore.football.livescores.footballscores.utils.PushDataParser
import com.livescore.football.livescores.footballscores.utils.PushNavigationExecutor
import com.livescore.football.livescores.footballscores.utils.PushPayload
import com.google.android.gms.ads.nativead.NativeAd
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob
import com.mallegan.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    companion object {
        private const val PREFS_NAME = "livescore_onboarding_prefs"
        private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
    }

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var deviceRegistrationManager: DeviceRegistrationManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    @Inject
    lateinit var pushClickTracker: PushClickTracker

    private lateinit var binding: ActivitySplashBinding
    private var interCallback: InterCallback? = null
    private var isTransitioning = false
    private var pendingPushPayload: PushPayload? = null

    override fun bind() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pendingPushPayload = PushDataParser.parseFromIntent(intent)
        pendingPushPayload?.let { payload ->
            android.util.Log.d("FCMService", "SplashActivity launched via push: push_id=${payload.pushId}, push_type=${payload.rawData["push_type"]}, navigation=${payload.navigation}")
        }
        trackPushClickIfNeeded()
        trackAppLaunch()

        lifecycleScope.launch(Dispatchers.IO) {
            RetentionTracker.checkAndTrackRetention(this@SplashActivity)
        }

        lifecycleScope.launch {
            deviceRegistrationManager.registerDevice(null)
        }

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrEmpty()) {
                        android.util.Log.d("FCMToken", "Device FCM Token: $token")
                        lifecycleScope.launch {
                            deviceRegistrationManager.registerDevice(token)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()

            runOnUiThread {
                initAdsAfterConfig()
            }
        }
    }

    private fun initAdsAfterConfig() {
        if (limitManager.isPremium()) {
            binding.frAdsBanner.visibility = View.GONE
        } else {
            binding.frAdsBanner.visibility = View.VISIBLE
        }

        interCallback = object : InterCallback() {
            override fun onAdClosedByUser() {
                super.onAdClosedByUser()
                LogEvent.log(this@SplashActivity, "inter_splash_view")

                val interId = remoteConfigManager.getAdId("inter_splash", getString(R.string.inter_splash))
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                    liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash"
                )
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                    liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash"
                )

                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.Companion.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full", R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                navigateAfterSplash()
                            }
                        })
                } else {
                    navigateAfterSplash()
                }

            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                val interId = remoteConfigManager.getAdId("inter_splash", getString(R.string.inter_splash))
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                    liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash", i?.code
                )

                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.Companion.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full",     R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                navigateAfterSplash()
                            }
                        })
                } else {
                    navigateAfterSplash()
                }
            }
        }
        loadAdsInter()
    }


    private fun loadAdsInter() {
        val isVIP = limitManager.isPremium()

        lifecycleScope.launch {
            for (progress in 0..99) {
                binding.tvProgressPercent.text = "$progress%"
                delay(if (isVIP) 10L else 150L)
            }
            if (isVIP) {
                navigateAfterSplash()
            }
        }

        if (isVIP) {
            return
        }

        val consentHelper = ConsentHelper.getInstance(this)
        if (!consentHelper.canLoadAndShowAds()) {
            consentHelper.reset()
        }

        consentHelper.obtainConsentAndShow(this) {
            // Load banner ad after consent is obtained
            if (!limitManager.isPremium()) {
                val bannerId = getRemoteAdId("banner_splash", R.string.banner_splash)
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                    liveScoreApiService, this@SplashActivity, "banner", bannerId, "Splash"
                )
                Admob.getInstance().loadBanner(this@SplashActivity, bannerId)
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                    liveScoreApiService, this@SplashActivity, "banner", bannerId, "Splash"
                )
                LogEvent.log(this@SplashActivity, "banner_splash_view")
            }

            // Load native language ad after consent is obtained
            if (pendingPushPayload == null && !isReturningUser()) {
                val nativeLanguageId = remoteConfigManager.getAdId("native_language", getString(R.string.native_language))
                if (nativeLanguageId.isNotEmpty() && !limitManager.isPremium()) {
                    Admob.getInstance().loadNativeAd(this@SplashActivity, nativeLanguageId, object : NativeCallback() {
                        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                            AdsConfig.nativeLanguage = nativeAd
                        }
                        override fun onAdFailedToLoad() {
                            AdsConfig.nativeLanguage = null
                        }
                    })
                }
            }

            // Load splash interstitial ad after consent is obtained
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    val interId = remoteConfigManager.getAdId("inter_splash", getString(R.string.inter_splash))
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                        liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash"
                    )

                    Admob.getInstance().loadSplashInterAdsFloor(
                        this@SplashActivity,
                        arrayListOf(
                            remoteConfigManager.getAdId(
                                "inter_splash_high",
                                getString(R.string.inter_splash_high)
                            ),
                            interId
                        ),
                        5000,
                        interCallback
                    )
                }
            }, 500)
        }
    }

//    private fun checkFullAds() {
//        if (SharePreferenceUtils.isOrganic(this)) {
//            val appToken = AppAdjustTokens.ADJUST_APP_TOKEN
//            val environment =
//                if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
//            val config = AdjustConfig(this, appToken, environment)
//
//            config.setOnAttributionChangedListener { attribution ->
//                Log.d("ADJUSTtracking", "network=${attribution.network}")
//                Log.d("ADJUST", "campaign=${attribution.campaign}")
//                Log.d("ADJUST", "trackerName=${attribution.trackerName}")
//
//                val isOrganic = attribution.network.isNullOrEmpty() || attribution.network.equals(
//                    "organic",
//                    ignoreCase = true
//                )
//
//                SharePreferenceUtils.setOrganic(
//                    applicationContext,
//                    isOrganic
//                )
//            }
//
//            Adjust.initSdk(config)
//        }
//    }


    private fun trackAppLaunch() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val count = prefs.getInt(KEY_APP_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_APP_LAUNCH_COUNT, count).apply()
    }

    private fun isReturningUser(): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_APP_LAUNCH_COUNT, 0) >= 2
    }

    private fun trackPushClickIfNeeded() {
        val pushId = pendingPushPayload?.pushId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            pushClickTracker.trackClick(pushId)
        }
    }

    private fun navigateAfterSplash() {
        if (isTransitioning) return
        isTransitioning = true

        val pushPayload = pendingPushPayload
        val intent = if (pushPayload != null) {
            PushNavigationExecutor.toDestinationIntent(this, pushPayload.navigation)
        } else if (isReturningUser()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LanguageActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPushPayload = PushDataParser.parseFromIntent(intent)
        pendingPushPayload?.let { payload ->
            android.util.Log.d("FCMService", "SplashActivity onNewIntent: push_id=${payload.pushId}, push_type=${payload.rawData["push_type"]}, navigation=${payload.navigation}")
        }
        trackPushClickIfNeeded()
        isTransitioning = false
    }


    protected override fun onStop() {
        super.onStop()
        Admob.getInstance().dismissLoadingDialog()
    }

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Splash"
        )
        Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, 5000)
    }


}