package com.livescore.football.livescores.footballscores


import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.ui.onboarding.SplashActivity
import com.mallegan.ads.util.AdsApplication
import com.mallegan.ads.util.AppOpenManager
import com.mallegan.ads.util.AppsFlyer

import dagger.hilt.android.HiltAndroidApp
import java.util.Collections
import java.util.concurrent.Executors
import javax.inject.Inject

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context

@HiltAndroidApp
class Application : AdsApplication() {

    @Inject
    lateinit var billingManager: BillingManager

    override fun enableAdsResume(): Boolean = true
    override fun getListTestDeviceId(): List<String>? = null
    override fun getResumeAdId(): String = getString(R.string.open_resume)
    override fun buildDebug(): Boolean? = null

    override fun onCreate() {
        super.onCreate()
        
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
            AppsFlyer.getInstance().initAppFlyer(this, getString(R.string.AF_DEV_KEY), true)
        }
    }


}
