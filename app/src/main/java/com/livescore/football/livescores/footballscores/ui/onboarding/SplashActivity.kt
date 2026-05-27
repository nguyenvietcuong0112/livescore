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
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV5
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
        setContentView(R.layout.activity_splash)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Calculate and track user retention milestones via Adjust
        RetentionTracker.checkAndTrackRetention(this)

        // Pre-fetch and synchronize the dynamic API key from remote config on startup
        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()
        }

        interCallback = object : InterCallback() {
            override fun onAdClosedByUser() {
                super.onAdClosedByUser()

                ActivityLoadNativeFullV5.open(
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
                ActivityLoadNativeFullV5.open(
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
        Thread(Runnable {
            for (progress in 0..99) {
                val currentProgress = progress
                runOnUiThread({
                    binding.tvProgressPercent.setText(currentProgress.toString() + "%")
                })
                try {
                    Thread.sleep(150)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
        val consentHelper = ConsentHelper.getInstance(this)
        if (!consentHelper.canLoadAndShowAds()) {
            consentHelper.reset()
        }
        consentHelper.obtainConsentAndShow(this) {
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
