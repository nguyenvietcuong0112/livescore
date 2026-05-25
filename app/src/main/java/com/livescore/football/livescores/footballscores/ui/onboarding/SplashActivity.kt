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
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV5
import com.mallegan.ads.callback.InterCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    private var interCallback: InterCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Pre-fetch and synchronize the dynamic API key from remote config on startup
        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()
        }

        // Delay 2 seconds for a premium brand feel, then show ad and route appropriately
        Handler(Looper.getMainLooper()).postDelayed({
            ActivityLoadNativeFullV5.open(
                this@SplashActivity,
                getString(R.string.native_full_splash_high),
                getString(R.string.native_full_splash),
                object : ActivityFullCallback {
                    override fun onResultFromActivityFull() {
                        startLanguage()
                    }
                })
        }, 2000)

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
    }

    private fun startLanguage() {
        val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val isCompleted = onboardingPrefs.getBoolean("onboarding_completed", false)

        val intent = if (isCompleted) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LanguageActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
