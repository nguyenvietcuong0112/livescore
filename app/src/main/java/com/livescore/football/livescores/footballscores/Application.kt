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
import com.livescore.football.livescores.footballscores.ui.permission.PermissionActivity
import com.livescore.football.livescores.footballscores.ui.splash.SplashActivity
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

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    override fun enableAdsResume(): Boolean = true
    override fun getListTestDeviceId(): List<String>? = null
    override fun getResumeAdId(): String {
        return getRemoteAdId("resume_open_app", R.string.resume_open_app)
    }

    override fun buildDebug(): Boolean? = null

    override fun onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val processName = android.app.Application.getProcessName()
            if (packageName != processName) {
                val suffix = processName.replace(":", "_")
                try {
                    android.webkit.WebView.setDataDirectorySuffix(suffix)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate()

        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.GlobalCrashHandler(
            context = this,
            apiService = liveScoreApiService,
            deviceId = deviceId
        )

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(IntroActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(IntroSlideshowActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(PermissionActivity::class.java)

        Executors.newSingleThreadExecutor().execute {
            try {
                FirebaseApp.initializeApp(this)
                FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
                AdjustHelper.init(
                    application = this,
                    appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                    iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
                    isDebug = BuildConfig.DEBUG
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(5000)
            try {
                gsmManager.loginGSM()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

//    fun initAdjust() {
//        if (!SharePreferenceUtils.isOrganic(applicationContext)) {
//
//        } else {
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
//                LogEvent.log(
//                    context = applicationContext,
//                    eventName = "adjust_attribution",
//                    params = mapOf(
//                        "network" to (attribution.network ?: "organic"),
//                        "campaign" to (attribution.campaign ?: ""),
//                        "tracker" to (attribution.trackerName ?: "")
//                    )
//                )
//
//                LogEvent.setUserProperty(
//                    applicationContext,
//                    "install_network",
//                    attribution.network ?: "organic"
//                )
//                LogEvent.setUserProperty(
//                    applicationContext,
//                    "install_campaign",
//                    attribution.campaign ?: ""
//                )
//                LogEvent.setUserProperty(
//                    applicationContext,
//                    "install_tracker",
//                    attribution.trackerName ?: ""
//                )
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
//            PreferenceManager.getInstance().putBoolean("is_admob_network_full_ads", true)
//            AdjustHelper.init(
//                application = this,
//                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
//                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
//                isDebug = BuildConfig.DEBUG
//            )
//        }
//    }
}
