package com.livescore.football.livescores.footballscores


import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.ui.onboarding.SplashActivity
import com.mallegan.ads.util.AdsApplication
import com.mallegan.ads.util.AppOpenManager

import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.Executors
import javax.inject.Inject

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context
import com.livescore.football.livescores.footballscores.data.remote.adjust.AppAdjustTokens
import com.livescore.football.livescores.footballscores.data.remote.getRemoteAdId

import com.mallegan.ads.util.AdjustHelper
import kotlinx.coroutines.launch

@HiltAndroidApp
class Application : AdsApplication() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var gsmManager: com.livescore.football.livescores.footballscores.data.remote.gsm.GsmManager

    override fun enableAdsResume(): Boolean = true
    override fun getListTestDeviceId(): List<String>? = null
    override fun getResumeAdId(): String {
        return getRemoteAdId("resume_open_app", R.string.resume_open_app)
    }
    override fun buildDebug(): Boolean? = null

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(5000)
            gsmManager.loginGSM()
        }
        
        // Ensure default language is English on first launch
        val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val savedLang = onboardingPrefs.getString("selected_language", null)
        if (savedLang == null) {
            onboardingPrefs.edit().putString("selected_language", "English").apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        } else {
            val localeCode = when (savedLang) {
                "Arabic" -> "ar"
                "English" -> "en"
                "French" -> "fr"
                "German" -> "de"
                "Hindi" -> "hi"
                "Indonesian" -> "id"
                "Italian" -> "it"
                "Japanese" -> "ja"
                "Portuguese" -> "pt"
                "Russian" -> "ru"
                "Spanish" -> "es"
                "Thai" -> "th"
                "Turkish" -> "tr"
                "Urdu" -> "ur"
                "Vietnamese" -> "vi"
                else -> "en"
            }
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (currentLocales.isEmpty || currentLocales.get(0)?.language != localeCode) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode))
            }
        }

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
        Executors.newSingleThreadExecutor().execute {
            FirebaseApp.initializeApp(this)
            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
            AdjustHelper.init(
                application = this,
                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
                isDebug = BuildConfig.DEBUG
            )
        }

        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
                // Do not hide here or touch decorView, let onResume handle it safely to avoid PhoneWindow null DecorView crashes
            }

            override fun onActivityStarted(activity: android.app.Activity) {}

            override fun onActivityResumed(activity: android.app.Activity) {
                hideSystemBars(activity)
                
                // Safely listen for focus changes to restore fullscreen when returning from ads, dialogs, etc.
                val decorView = activity.window?.peekDecorView()
                if (decorView != null && decorView.tag != "focus_listener_attached") {
                    decorView.tag = "focus_listener_attached"
                    decorView.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
                        if (hasFocus) {
                            hideSystemBars(activity)
                        }
                    }
                }
            }

            override fun onActivityPaused(activity: android.app.Activity) {}

            override fun onActivityStopped(activity: android.app.Activity) {}

            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}

            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun hideSystemBars(activity: android.app.Activity) {
        val window = activity.window ?: return
        val decorView = window.peekDecorView() ?: return // Safely avoid PhoneWindow's internal null DecorView crash
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                val insetsController = window.insetsController
                if (insetsController != null) {
                    insetsController.hide(android.view.WindowInsets.Type.systemBars())
                    insetsController.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility =
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
