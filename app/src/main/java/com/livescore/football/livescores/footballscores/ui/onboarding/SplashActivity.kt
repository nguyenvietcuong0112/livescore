package com.livescore.football.livescores.footballscores.ui.onboarding

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.LoadAdError
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.data.remote.adjust.RetentionTracker
import com.livescore.football.livescores.footballscores.data.remote.getRemoteAdId
import com.livescore.football.livescores.footballscores.databinding.ActivitySplashBinding
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV1
import com.livescore.football.livescores.footballscores.utils.SharePreferenceUtils
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob
import com.mallegan.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivitySplashBinding


    private var interCallback: InterCallback? = null

    override fun bind() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        checkFullAds()
        RetentionTracker.checkAndTrackRetention(this)

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
            Admob.getInstance()
                .loadBanner(this, getRemoteAdId("banner_splash", R.string.banner_splash))
        }

        interCallback = object : InterCallback() {
            override fun onAdClosedByUser() {
                super.onAdClosedByUser()
                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full", R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                startLanguage()
                            }
                        })
                } else {
                    startLanguage()
                }

            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full", R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                startLanguage()
                            }
                        })
                } else {
                    startLanguage()
                }
            }
        }
        loadAdsInter()
    }


    private fun loadAdsInter() {
        val isVIP = limitManager.isPremium()

        Thread(Runnable {
            for (progress in 0..99) {
                val currentProgress = progress
                runOnUiThread {
                    binding.tvProgressPercent.text = "$currentProgress%"
                }
                try {
                    Thread.sleep(if (isVIP) 10 else 150)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            if (isVIP) {
                runOnUiThread {
                    startLanguage()
                }
            }
        }).start()

        if (isVIP) {
            return
        }

        val consentHelper = ConsentHelper.getInstance(this)
        if (!consentHelper.canLoadAndShowAds()) {
            consentHelper.reset()
        }

        consentHelper.obtainConsentAndShow(this) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    Admob.getInstance().loadSplashInterAdsFloor(
                        this@SplashActivity,
                        arrayListOf(
                            remoteConfigManager.getAdId(
                                "inter_splash_high",
                                getString(R.string.inter_splash_high)
                            ),
                            remoteConfigManager.getAdId(
                                "inter_splash",
                                getString(R.string.inter_splash)
                            )
                        ),
                        3000,
                        interCallback
                    )
                }
            }, 2000)
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


    private fun startLanguage() {
        val intent = Intent(this, LanguageActivity::class.java)
        startActivity(intent)
        finish()
    }


    protected override fun onStop() {
        super.onStop()
        Admob.getInstance().dismissLoadingDialog()
    }

    override fun onResume() {
        super.onResume()
        Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, 1000)
    }


}
