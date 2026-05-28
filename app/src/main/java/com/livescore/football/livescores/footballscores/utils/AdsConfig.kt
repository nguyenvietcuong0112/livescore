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
                        lastInterAdShowTime = System.currentTimeMillis()
                        ActivityLoadNativeFullV2.open(
                            activity,
                            activity.getString(R.string.native_all),
                            object : ActivityFullCallback {
                                override fun onResultFromActivityFull() {
                                    onAdClosed()
                                }
                            }
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError?) {
                        super.onAdFailedToLoad(error)
                        ActivityLoadNativeFullV2.open(
                            activity,
                            activity.getString(R.string.native_all),
                            object : ActivityFullCallback {
                                override fun onResultFromActivityFull() {
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
