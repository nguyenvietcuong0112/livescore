package com.livescore.football.livescores.footballscores.ui.admob

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    // Test Ad Unit IDs
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    // Frequency capping: show interstitial every 3 clicks
    private var clickCount = 0
    private const val CLICK_THRESHOLD = 3

    fun loadBannerAd(context: Context, container: FrameLayout) {
        try {
            val adView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
            }
            container.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: log error, hide container or show mock placeholder
        }
    }

    fun preloadInterstitial(context: Context) {
        if (mInterstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            }
        )
    }

    fun showInterstitialWithCapping(activity: Activity, onDismiss: () -> Unit) {
        clickCount++
        if (clickCount >= CLICK_THRESHOLD && mInterstitialAd != null) {
            clickCount = 0 // Reset
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    preloadInterstitial(activity) // Preload next
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mInterstitialAd = null
                    onDismiss()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            // Capped or ad not loaded, execute action immediately
            onDismiss()
        }
    }
}
