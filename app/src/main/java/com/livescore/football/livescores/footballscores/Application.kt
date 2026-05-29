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
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroSlideshowActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.PermissionActivity

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

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(IntroActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(IntroSlideshowActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(PermissionActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(5000)
            gsmManager.loginGSM()
        }
        
        val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val savedLang = onboardingPrefs.getString("selected_language", null)
        if (savedLang == null) {
            onboardingPrefs.edit().putString("selected_language", "English").apply()
            com.livescore.football.livescores.footballscores.utils.SystemUtil.saveLocale(this, "en")
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
            com.livescore.football.livescores.footballscores.utils.SystemUtil.saveLocale(this, localeCode)
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (currentLocales.isEmpty || currentLocales.get(0)?.language != localeCode) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode))
            }
        }

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

    }
}
