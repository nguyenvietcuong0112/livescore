package com.livescore.football.livescores.footballscores


import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import androidx.core.os.LocaleListCompat
import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.remote.adjust.AppAdjustTokens
import com.livescore.football.livescores.footballscores.data.remote.getRemoteAdId
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroSlideshowActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.PermissionActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.SplashActivity
import com.livescore.football.livescores.footballscores.utils.SharePreferenceUtils
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.livescore.football.livescores.footballscores.utils.SystemUtil
import com.mallegan.ads.util.AdjustHelper
import com.mallegan.ads.util.AdsApplication
import com.mallegan.ads.util.AppOpenManager
import com.mallegan.ads.util.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

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
        AppOpenManager.getInstance()
            .disableAppResumeWithActivity(IntroSlideshowActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(PermissionActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(5000)
            gsmManager.loginGSM()
        }

        val onboardingPrefs =
            getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val savedLang = onboardingPrefs.getString("selected_language", null)
        if (savedLang == null) {
            onboardingPrefs.edit().putString("selected_language", "English").apply()
            SystemUtil.saveLocale(this, "en")
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
            SystemUtil.saveLocale(
                this,
                localeCode
            )
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (currentLocales.isEmpty || currentLocales.get(0)?.language != localeCode) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode))
            }
        }

        Executors.newSingleThreadExecutor().execute {
            FirebaseApp.initializeApp(this)
            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))

        }
        initAdjust()

    }

    fun initAdjust() {
        if (!SharePreferenceUtils.isOrganic(applicationContext)) {
            AdjustHelper.init(
                application = this,
                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
                isDebug = BuildConfig.DEBUG
            )
        } else {
            val appToken = AppAdjustTokens.ADJUST_APP_TOKEN
            val environment =
                if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
            val config = AdjustConfig(this, appToken, environment)

            config.setOnAttributionChangedListener { attribution ->
                Log.d("ADJUSTtracking", "network=${attribution.network}")
                Log.d("ADJUST", "campaign=${attribution.campaign}")
                Log.d("ADJUST", "trackerName=${attribution.trackerName}")

                LogEvent.log(
                    context = applicationContext,
                    eventName = "adjust_attribution",
                    params = mapOf(
                        "network" to (attribution.network ?: "organic"),
                        "campaign" to (attribution.campaign ?: ""),
                        "tracker" to (attribution.trackerName ?: "")
                    )
                )

                LogEvent.setUserProperty(
                    applicationContext,
                    "install_network",
                    attribution.network ?: "organic"
                )
                LogEvent.setUserProperty(
                    applicationContext,
                    "install_campaign",
                    attribution.campaign ?: ""
                )
                LogEvent.setUserProperty(
                    applicationContext,
                    "install_tracker",
                    attribution.trackerName ?: ""
                )

                val isOrganic = attribution.network.isNullOrEmpty() || attribution.network.equals(
                    "organic",
                    ignoreCase = true
                )

                SharePreferenceUtils.setOrganic(
                    applicationContext,
                    isOrganic
                )
            }

            Adjust.initSdk(config)
            PreferenceManager.getInstance().putBoolean("is_admob_network_full_ads", true)
            AdjustHelper.init(
                application = this,
                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
                isDebug = BuildConfig.DEBUG
            )
        }
    }
}
