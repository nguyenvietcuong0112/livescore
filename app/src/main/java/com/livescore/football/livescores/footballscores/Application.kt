package com.livescore.football.livescores.footballscores


import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.livescore.football.livescores.footballscores.ui.onboarding.SplashActivity
import com.mallegan.ads.util.AdsApplication
import com.mallegan.ads.util.AppOpenManager
import com.mallegan.ads.util.AppsFlyer

import dagger.hilt.android.HiltAndroidApp
import java.util.Collections
import java.util.concurrent.Executors

@HiltAndroidApp
class Application : AdsApplication() {
    override fun enableAdsResume(): Boolean = true
    override fun getListTestDeviceId(): List<String>? = null
    override fun getResumeAdId(): String = getString(R.string.open_resume)
    override fun buildDebug(): Boolean? = null

    override fun onCreate() {
        super.onCreate()
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
        Executors.newSingleThreadExecutor().execute {
            FirebaseApp.initializeApp(this)
            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
            AppsFlyer.getInstance().initAppFlyer(this, getString(R.string.AF_DEV_KEY), true)
        }
    }


}
