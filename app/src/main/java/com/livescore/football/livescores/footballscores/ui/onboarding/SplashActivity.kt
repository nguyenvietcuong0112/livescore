package com.livescore.football.livescores.footballscores.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.LoadAdError
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.data.remote.adjust.RetentionTracker
import com.livescore.football.livescores.footballscores.databinding.ActivitySplashBinding
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import android.content.Context
import android.view.View
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV1
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob
import com.mallegan.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivitySplashBinding


    private var interCallback: InterCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (limitManager.isPremium()) {
            binding.frAdsBanner.visibility = View.GONE
        } else {
            binding.frAdsBanner.visibility = View.VISIBLE
            Admob.getInstance().loadBanner(this, getString(R.string.banner_splash))
        }

        RetentionTracker.checkAndTrackRetention(this)

        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()
        }

        interCallback = object : InterCallback() {
            override fun onAdClosedByUser() {
                super.onAdClosedByUser()
                ActivityLoadNativeFullV1.open(
                    this@SplashActivity,
                    getString(R.string.native_splash_full_high),
                    getString(R.string.native_splash_full),
                    object : ActivityFullCallback {
                        override fun onResultFromActivityFull() {
                            startLanguage()
                        }
                    })
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                ActivityLoadNativeFullV1.open(
                    this@SplashActivity,
                    getString(R.string.native_splash_full_high),
                    getString(R.string.native_splash_full),
                    object : ActivityFullCallback {
                        override fun onResultFromActivityFull() {
                            startLanguage()
                        }
                    })
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
                            getString(R.string.inter_splash_high),
                            getString(R.string.inter_splash)
                        ),
                        3000,
                        interCallback
                    )
                }
            }, 2000)
        }
    }


    private fun startLanguage() {
        val intent = Intent(this, LanguageActivity::class.java)
        startActivity(intent)
        finish()
    }


    protected override fun onStop() {
        super.onStop()
        Admob.getInstance().dismissLoadingDialog()
    }

    protected override fun onResume() {
        super.onResume()
        Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, 1000)
    }

}
