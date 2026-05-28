package com.livescore.football.livescores.footballscores.utils

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob

object AdsConfig {

    var nativeIntro1: NativeAd? = null

    var lastInterAdShowTime: Long = 0L

    fun showInterClickAd(activity: AppCompatActivity, onAdClosed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val isEnabled = try {
            RemoteConfigManager.getInstance().isInterClickEnabled()
        } catch (e: Exception) {
            true
        }

        val interClickId = try {
            RemoteConfigManager.getInstance()
                .getAdId("inter_click", activity.getString(R.string.inter_click))
        } catch (e: Exception) {
            activity.getString(R.string.inter_click)
        }

        val nativeAllId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_all", activity.getString(R.string.native_all))
        } catch (e: Exception) {
            activity.getString(R.string.native_all)
        }

        if (isEnabled && currentTime - lastInterAdShowTime >= 35000L) {
            Admob.getInstance().loadAndShowInter(
                activity,
                interClickId,
                0,
                30000,
                object : InterCallback() {
                    override fun onAdClosed() {
                        super.onAdClosed()
                        lastInterAdShowTime = System.currentTimeMillis()
                        ActivityLoadNativeFullV2.open(
                            activity,
                            nativeAllId,
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
                            nativeAllId,
                            object : ActivityFullCallback {
                                override fun onResultFromActivityFull() {
                                    onAdClosed()
                                }
                            }
                        )
                    }
                }
            )
        }

    }
}
