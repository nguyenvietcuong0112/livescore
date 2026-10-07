package com.livescore.football.livescores.footballscores

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.remote.adjust.AppAdjustTokens
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.utils.RemoteConfigs
import com.livescore.football.livescores.footballscores.utils.SharePreferenceUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class Application : android.app.Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var gsmManager: com.livescore.football.livescores.footballscores.data.remote.gsm.GsmManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

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

        val savedLangCode = com.cscmobi.libraryads.commons.sharepreference.CSCSPF(this).language_code_selected
        if (!savedLangCode.isNullOrBlank()) {
            com.livescore.football.livescores.footballscores.utils.SystemUtil.setLocale(this)
        }

        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.GlobalCrashHandler(
            context = this,
            apiService = liveScoreApiService,
            deviceId = deviceId
        )

        initCSCAds()

        Executors.newSingleThreadExecutor().execute {
            try {
                FirebaseApp.initializeApp(this)
                FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
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

    private fun initCSCAds() {
        try {
            val cscLibrary = CSCApplication(this, RemoteConfigs, BuildConfig.DEBUG)
            cscLibrary.initSdk(
                adjustAppToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                gsmAppId = com.livescore.football.livescores.footballscores.data.remote.gsm.GsmConfig.GSM_APP_ID,
                splashConfig = SplashConfig(
                    uiSplashConfig = UiSplashConfig(
                        resLayout = R.layout.activity_splash,
                        showFOForever = false,
                        homeActivity = MainActivity::class.java,
                        timeout = 30_000
                    ),
                    adsSplashConfig = AdsSplashConfig(
                        bannerId = getString(R.string.banner_splash),
                        interHighId = getString(R.string.inter_splash_high),
                        interAllId = getString(R.string.inter_splash),
                        nativeFullLayout = R.layout.layout_native_full,
                        admobAOAId = getString(R.string.resume_open_app),
                        isCheckOrganicUser = true
                    )
                ),
                languageConfig = LanguageConfig(
                    uiLanguageConfig = UiLanguageConfig(
                        resLayout = R.layout.activity_language_app,
                        itemLangDefault = R.layout.item_select_language_default,
                        itemLangSelected = R.layout.item_select_language_selected,
                        listLanguage = com.livescore.football.livescores.footballscores.utils.EnumSelectLanguage.toLanguageModelList(),
                        languageSetting = object : LanguageSetting {
                            override fun onDone(activity: Activity) {
                                val code = com.cscmobi.libraryads.commons.sharepreference.CSCSPF(activity).language_code_selected
                                val finalCode = if (!code.isNullOrBlank()) code else com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(activity)
                                Log.d("LanguageDebug", "App languageSetting.onDone: code=$code, finalCode=$finalCode")
                                com.livescore.football.livescores.footballscores.utils.SystemUtil.changeLang(finalCode, activity)
                            }
                        }
                    ),
                    adsLanguageConfig = AdsLanguageConfig(
                        nativeLangHighId = getString(R.string.native_language_high),
                        nativeLangId = getString(R.string.native_language),
                        nativeLangClickHighId = getString(R.string.native_language_high_click),
                        nativeLangClickId = getString(R.string.native_language_click),
                        layoutNative = R.layout.layout_native_media,
                        layoutNativeClick = R.layout.layout_native_media_click
                    )
                ),
                obConfig = OBConfig(
                    uiOBConfig = UiOBConfig(
                        resFragmentOB1 = R.layout.fragment_intro1,
                        resFragmentOB2 = R.layout.fragment_intro2,
                        resFragmentOB3 = R.layout.fragment_intro3,
                        resFragmentOB4 = R.layout.fragment_intro4,
                        resFragmentOBAdFull = R.layout.fragment_ob_ad_full,
                        activityCallback = object : OnActivityCallBack {
                            override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                                val savedLang = com.cscmobi.libraryads.commons.sharepreference.CSCSPF(activity).language_code_selected
                                if (!savedLang.isNullOrBlank()) {
                                    com.livescore.football.livescores.footballscores.utils.SystemUtil.changeLang(savedLang, activity)
                                }
                                val intent = android.content.Intent(activity, com.livescore.football.livescores.footballscores.ui.permission.PermissionActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                activity.startActivity(intent)
                                activity.finish()
                            }
                        }
//                        nextOBActivity = com.livescore.football.livescores.footballscores.ui.permission.PermissionActivity::class.java
                    ),
                    adsOBConfig = AdsOBConfig(
                        nativeOB1Id = getString(R.string.native_onboarding_1),
                        nativeOB4Id = getString(R.string.native_onboarding_4),
                        nativeOBFull12Id = getString(R.string.native_onboarding_full_1),
                        nativeOBFull23Id = getString(R.string.native_onboarding_full_2),
                        layoutNativeOB1 = R.layout.layout_native_media,
                        layoutNativeOB4 = R.layout.layout_native_media,
                        layoutNativeFullOB = R.layout.admob_layout_native_full,
                        isLoadNativeOBInLanguage = true
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
