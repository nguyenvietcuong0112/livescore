package com.livescore.football.livescores.footballscores.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.LoadAdError
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.databinding.ActivitySplashBinding
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
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

    private lateinit var binding: ActivitySplashBinding


    private var interCallback: InterCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fetch and synchronize the dynamic API key from remote config on startup
        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()
        }

        interCallback = object : InterCallback() {
            override fun onAdClosedByUser() {
                super.onAdClosedByUser()

                ActivityLoadNativeFullV5.open(
                    this@SplashActivity,
                    getString(R.string.native_full_splash_high),
                    getString(R.string.native_full_splash),
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
                    getString(R.string.native_full_splash_high),
                    getString(R.string.native_full_splash),
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
        val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val isCompleted = onboardingPrefs.getBoolean("onboarding_completed", false)

//        val intent = if (isCompleted) {
//            Intent(this, MainActivity::class.java)
//        } else {
            Intent(this, LanguageActivity::class.java)
//        }
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
