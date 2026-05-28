package com.livescore.football.livescores.footballscores.utils

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.livescore.football.livescores.footballscores.R
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob

object AdsConfig {

    var nativeIntro1: NativeAd? = null

    // Track the last time the interstitial click ad was displayed globally
    var lastInterAdShowTime: Long = 0L

    fun showInterClickAd(activity: AppCompatActivity, onAdClosed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterAdShowTime >= 35000L) {
            Admob.getInstance().loadAndShowInter(
                activity,
                activity.getString(R.string.inter_click),
                0,
                30000,
                object : InterCallback() {
                    override fun onAdClosed() {
                        super.onAdClosed()
                        if (activity.isDestroyed || activity.isFinishing || activity.supportFragmentManager.isStateSaved) return
                        lastInterAdShowTime = System.currentTimeMillis()
                        ActivityLoadNativeFullV1.open(
                            activity,
                            activity.getString(R.string.native_splash_full_high),
                            activity.getString(R.string.native_splash_full),
                            object : ActivityFullCallback {
                                override fun onResultFromActivityFull() {
                                    if (activity.isDestroyed || activity.isFinishing || activity.supportFragmentManager.isStateSaved) return
                                    onAdClosed()
                                }
                            }
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError?) {
                        super.onAdFailedToLoad(error)
                        if (activity.isDestroyed || activity.isFinishing || activity.supportFragmentManager.isStateSaved) return
                        ActivityLoadNativeFullV1.open(
                            activity,
                            activity.getString(R.string.native_splash_full_high),
                            activity.getString(R.string.native_splash_full),
                            object : ActivityFullCallback {
                                override fun onResultFromActivityFull() {
                                    if (activity.isDestroyed || activity.isFinishing || activity.supportFragmentManager.isStateSaved) return
                                    onAdClosed()
                                }
                            }
                        )
                    }
                }
            )
        } else {
            // Cap not reached: perform transition directly
            onAdClosed()
        }
    }
}
