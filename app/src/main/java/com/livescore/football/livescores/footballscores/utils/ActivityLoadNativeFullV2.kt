package com.livescore.football.livescores.footballscores.utils

import android.os.CountDownTimer
import android.view.LayoutInflater
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityNativeFullBinding
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob


import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActivityLoadNativeFullV2 : AbsBaseActivity() {
    var binding: ActivityNativeFullBinding? = null

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    override fun bind() {
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_DARK
        )
        binding = ActivityNativeFullBinding.inflate(getLayoutInflater())
        setContentView(binding?.getRoot())

        val adId: kotlin.String? = if (getIntent().hasExtra(ActivityLoadNativeFullV2.Companion.NATIVE_FUll_AD_ID)) {
            getIntent().getStringExtra(ActivityLoadNativeFullV2.Companion.NATIVE_FUll_AD_ID)
        } else {
            ""
        }

        loadNativeFull(adId)
    }


    private fun loadNativeFull(adId: kotlin.String?) {
        val nonNullId = adId ?: ""

        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
            liveScoreApiService, this, "native", nonNullId, "NativeSplashFullV2"
        )

        Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                    liveScoreApiService, this@ActivityLoadNativeFullV2, "native", nonNullId, "NativeSplashFullV2", null
                )
                binding?.frAdsFull?.setVisibility(android.view.View.GONE)
                if (ActivityLoadNativeFullV2.Companion.callback != null) {
                    ActivityLoadNativeFullV2.Companion.callback!!.onResultFromActivityFull()
                }
                finish()
            }

            override fun onNativeAdLoaded(nativeAd: com.google.android.gms.ads.nativead.NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                    liveScoreApiService, this@ActivityLoadNativeFullV2, "native", nonNullId, "NativeSplashFullV2"
                )

                nativeAd?.setOnPaidEventListener { adValue ->
                    val ecpm = adValue.valueMicros / 1000.0
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                        liveScoreApiService, this@ActivityLoadNativeFullV2, "native", nonNullId, "NativeSplashFullV2", ecpm
                    )
                }

                val adView = LayoutInflater.from(this@ActivityLoadNativeFullV2)
                    .inflate(
                        R.layout.native_full_language,
                        null
                    ) as com.google.android.gms.ads.nativead.NativeAdView
                binding?.frAdsFull?.removeAllViews()
                binding?.frAdsFull?.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                val closeButton = adView.findViewById<android.widget.ImageView>(R.id.close)
                val mediaView =
                    adView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
                closeButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> mediaView.performClick() })
                closeButton.postDelayed({
                    closeButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
                        if (ActivityLoadNativeFullV2.Companion.callback != null) {
                            ActivityLoadNativeFullV2.Companion.callback!!.onResultFromActivityFull()
                        }
                        finish()
                    })
                }, 2000)
            }
        })
    }

    var count: kotlin.Int = 0

    protected override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "NativeSplashFullV2"
        )
        count++
        if (count >= 2) {
            if (ActivityLoadNativeFullV2.Companion.callback != null) {
                ActivityLoadNativeFullV2.Companion.callback!!.onResultFromActivityFull()
            }
            finish()
        }
    }

    companion object {
        const val NATIVE_FUll_AD_ID: kotlin.String = "native_full_ad_id"

        private var callback: ActivityFullCallback? = null

        fun open(context: android.content.Context, id: kotlin.String?, cb: ActivityFullCallback?) {
            ActivityLoadNativeFullV2.Companion.callback = cb
            val intent = android.content.Intent(context, ActivityLoadNativeFullV2::class.java)
            intent.putExtra(ActivityLoadNativeFullV2.Companion.NATIVE_FUll_AD_ID, id)
            context.startActivity(intent)
        }
    }
}