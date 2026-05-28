package com.livescore.football.livescores.footballscores.utils

import android.os.CountDownTimer
import android.view.LayoutInflater
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityNativeFullBinding
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob


class ActivityLoadNativeFullV2 : AbsBaseActivity() {
    var binding: ActivityNativeFullBinding? = null
    override fun bind() {
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_DARK
        )
        binding = ActivityNativeFullBinding.inflate(getLayoutInflater())
        setContentView(binding?.getRoot())

        val adId: kotlin.String?
        if (getIntent().hasExtra(ActivityLoadNativeFullV2.Companion.NATIVE_FUll_AD_ID)) {
            adId = getIntent().getStringExtra(ActivityLoadNativeFullV2.Companion.NATIVE_FUll_AD_ID)
        } else {
            adId = getString("".toInt())
        }

        loadNativeFull(adId)
    }


    private fun loadNativeFull(adId: kotlin.String?) {
        Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                binding?.frAdsFull?.setVisibility(android.view.View.GONE)
                if (ActivityLoadNativeFullV2.Companion.callback != null) {
                    ActivityLoadNativeFullV2.Companion.callback!!.onResultFromActivityFull()
                }
                finish()
            }

            override fun onNativeAdLoaded(nativeAd: com.google.android.gms.ads.nativead.NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                val adView = LayoutInflater.from(this@ActivityLoadNativeFullV2)
                    .inflate(
                        R.layout.native_full_language,
                        null
                    ) as com.google.android.gms.ads.nativead.NativeAdView
                val closeButton = adView.findViewById<android.widget.ImageView>(R.id.close)
                val mediaView =
                    adView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
                closeButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> mediaView.performClick() })
                object : CountDownTimer(2000, 1000) {
                    override fun onTick(millisUntilFinished: kotlin.Long) {
                    }

                    override fun onFinish() {
                        closeButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
                            if (ActivityLoadNativeFullV2.Companion.callback != null) {
                                ActivityLoadNativeFullV2.Companion.callback!!.onResultFromActivityFull()
                            }
                            finish()
                        })
                    }
                }.start()
                binding?.frAdsFull?.removeAllViews()
                binding?.frAdsFull?.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
            }
        })
    }

    var count: kotlin.Int = 0

    protected override fun onResume() {
        super.onResume()
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