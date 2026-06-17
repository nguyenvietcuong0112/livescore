package com.livescore.football.livescores.footballscores.utils

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

object AdsConfig {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AdsConfigEntryPoint {
        fun requestLimitManager(): RequestLimitManager
    }

    var nativeIntro1: NativeAd? = null
    var nativeLanguage: NativeAd? = null

    var lastInterAdShowTime: Long = 0L

    fun showInterClickAd(activity: AppCompatActivity, onAdClosedAction: () -> Unit) {
        val limitManager = try {
            EntryPoints.get(
                activity.applicationContext,
                AdsConfigEntryPoint::class.java
            ).requestLimitManager()
        } catch (e: Exception) {
            null
        }

        if (limitManager != null && limitManager.isPremium()) {
            onAdClosedAction()
            return
        }

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
                        if (!SharePreferenceUtils.isOrganic(activity)) {
                            ActivityLoadNativeFullV2.open(
                                activity,
                                nativeAllId,
                                object : ActivityFullCallback {
                                    override fun onResultFromActivityFull() {
                                        onAdClosedAction()
                                    }
                                }
                            )
                        } else {
                            onAdClosedAction()
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError?) {
                        super.onAdFailedToLoad(error)
                        if (!SharePreferenceUtils.isOrganic(activity)) {
                            ActivityLoadNativeFullV2.open(
                                activity,
                                nativeAllId,
                                object : ActivityFullCallback {
                                    override fun onResultFromActivityFull() {
                                        onAdClosedAction()
                                    }
                                }
                            )
                        } else {
                            onAdClosedAction()
                        }
                    }
                }
            )
        } else {
            onAdClosedAction()
        }

    }
}
