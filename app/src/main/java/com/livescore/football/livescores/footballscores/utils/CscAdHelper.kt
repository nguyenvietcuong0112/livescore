package com.livescore.football.livescores.footballscores.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.commons.adjust.trackingRevenueAd

abstract class NativeCallback {
    open fun onNativeAdLoaded(nativeAd: NativeAd?) {}
    open fun onAdFailedToLoad() {}
}

abstract class InterCallback {
    open fun onInterstitialLoad(interstitialAd: InterstitialAd) {}
    open fun onAdClosed() {}
    open fun onAdClosedByUser() {}
    open fun onAdFailedToLoad(error: LoadAdError?) {}
}

object CscAdHelper {

    fun loadNativeAd(context: Context, adId: String?, callback: NativeCallback) {
        if (adId.isNullOrBlank()) {
            callback.onAdFailedToLoad()
            return
        }
        try {
            val adLoader = AdLoader.Builder(context, adId)
                .forNativeAd { nativeAd ->
                    try {
                        trackingRevenueAd(nativeAd)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    callback.onNativeAdLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        callback.onAdFailedToLoad()
                    }
                })
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onAdFailedToLoad()
        }
    }

    fun loadNativeAds(context: Context, adId: String?, count: Int = 1, callback: NativeCallback) {
        loadNativeAd(context, adId, callback)
    }

    fun pushAdsToViewCustom(nativeAd: NativeAd?, adView: NativeAdView?) {
        if (nativeAd == null || adView == null) return

        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        if (headlineView != null) {
            headlineView.text = nativeAd.headline
            adView.headlineView = headlineView
        }

        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        if (bodyView != null) {
            if (nativeAd.body == null) {
                bodyView.visibility = View.INVISIBLE
            } else {
                bodyView.visibility = View.VISIBLE
                bodyView.text = nativeAd.body
            }
            adView.bodyView = bodyView
        }

        val ctaView = adView.findViewById<TextView>(R.id.ad_call_to_action)
        if (ctaView != null) {
            if (nativeAd.callToAction == null) {
                ctaView.visibility = View.INVISIBLE
            } else {
                ctaView.visibility = View.VISIBLE
                ctaView.text = nativeAd.callToAction
            }
            adView.callToActionView = ctaView
        }

        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        if (iconView != null) {
            if (nativeAd.icon == null) {
                iconView.visibility = View.GONE
            } else {
                iconView.setImageDrawable(nativeAd.icon?.drawable)
                iconView.visibility = View.VISIBLE
            }
            adView.iconView = iconView
        }

        val mediaView = adView.findViewById<MediaView>(R.id.media_view)
            ?: adView.findViewById<MediaView>(R.id.ad_media)
        if (mediaView != null) {
            adView.mediaView = mediaView
        }

        adView.setNativeAd(nativeAd)

        // Delegate to library binder to ensure all library-specific bindings are executed
        try {
            com.cscmobi.libraryads.ads.native_ads.renderer.DefaultNativeViewBinder().bind(adView, nativeAd)
        } catch (_: Exception) {}

        // Ensure content is visible and shimmer is hidden
        adView.findViewById<View>(R.id.ad_content_view)?.visibility = View.VISIBLE
        adView.findViewById<View>(R.id.shimmer_view)?.visibility = View.GONE
    }

    fun loadAndShowInter(
        activity: Activity,
        adId: String,
        delay: Long = 0L,
        timeout: Long = 30000L,
        callback: InterCallback
    ) {
        if (adId.isBlank()) {
            callback.onAdFailedToLoad(null)
            return
        }
        CSCInter.loadAndShowInter(
            activity = activity,
            adId = adId,
            timeDelay = delay,
            timeOut = timeout,
            canShowId = true,
            onShown = {},
            nextAction = { isSuccess ->
                if (isSuccess) {
                    callback.onAdClosed()
                    callback.onAdClosedByUser()
                } else {
                    callback.onAdFailedToLoad(null)
                }
            }
        )
    }

    fun dismissLoadingDialog() {
        // No-op or managed by CSCInter
    }

    fun onCheckShowSplashWhenFail(activity: Activity, callback: InterCallback?, timeout: Long = 5000L) {
        // Handled directly via CSCInter.loadAndShowInterSplash timeout
    }
}

/**
 * Compatibility facade that transparently maps legacy Admob calls to CscAdHelper / CSC Library.
 */
object Admob {
    fun getInstance(): Admob = this

    fun loadNativeAd(context: Context, adId: String?, callback: NativeCallback) {
        CscAdHelper.loadNativeAd(context, adId, callback)
    }

    fun loadNativeAds(context: Context, adId: String?, count: Int = 1, callback: NativeCallback) {
        CscAdHelper.loadNativeAds(context, adId, count, callback)
    }

    fun pushAdsToViewCustom(nativeAd: NativeAd?, adView: NativeAdView?) {
        CscAdHelper.pushAdsToViewCustom(nativeAd, adView)
    }

    fun loadAndShowInter(
        activity: Activity,
        adId: String,
        delay: Long = 0L,
        timeout: Long = 30000L,
        callback: InterCallback
    ) {
        CscAdHelper.loadAndShowInter(activity, adId, delay, timeout, callback)
    }

    fun dismissLoadingDialog() {
        CscAdHelper.dismissLoadingDialog()
    }

    fun onCheckShowSplashWhenFail(activity: Activity, callback: InterCallback?, timeout: Long = 5000L) {
        CscAdHelper.onCheckShowSplashWhenFail(activity, callback, timeout)
    }
}
