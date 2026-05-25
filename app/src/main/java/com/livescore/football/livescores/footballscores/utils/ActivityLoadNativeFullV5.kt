package com.livescore.football.livescores.footballscores.utils

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityNativeFullBinding
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob

class ActivityLoadNativeFullV5 : AbsBaseActivity() {
    var binding: ActivityNativeFullBinding? = null
    public override fun bind() {
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_DARK
        )
        binding = ActivityNativeFullBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val adIdHigh = if (intent.hasExtra(NATIVE_FUll_AD_ID_HIGH)) {
            intent.getStringExtra(NATIVE_FUll_AD_ID_HIGH)
        } else {
            ""
        }
        val adIdLow = if (intent.hasExtra(NATIVE_FUll_AD_ID)) {
            intent.getStringExtra(NATIVE_FUll_AD_ID)
        } else {
            ""
        }
        loadNativeFull(adIdHigh, adIdLow)
    }


    private fun loadNativeFull(adIdHigh: String?, adIdLow: String?) {
        Admob.getInstance().loadNativeAds(this, adIdHigh, 1, object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                Admob.getInstance().loadNativeAds(
                    this@ActivityLoadNativeFullV5,
                    adIdLow,
                    1,
                    object : NativeCallback() {
                        override fun onAdFailedToLoad() {
                            super.onAdFailedToLoad()
                            binding?.frAdsFull?.visibility = View.GONE
                            callback?.onResultFromActivityFull()
                            finish()
                        }

                        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                            super.onNativeAdLoaded(nativeAd)
                            val adView = LayoutInflater.from(this@ActivityLoadNativeFullV5)
                                .inflate(R.layout.native_full_language, null) as NativeAdView
                            val closeButton = adView.findViewById<ImageView>(R.id.close)
                            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                            closeButton.setOnClickListener(View.OnClickListener { v: View? -> mediaView.performClick() })
                            object : CountDownTimer(5000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                }

                                override fun onFinish() {
                                    closeButton.setOnClickListener(View.OnClickListener { v: View? ->
                                        callback?.onResultFromActivityFull()
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

            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                val adView = LayoutInflater.from(this@ActivityLoadNativeFullV5)
                    .inflate(R.layout.native_full_language, null) as NativeAdView
                val closeButton = adView.findViewById<ImageView>(R.id.close)
                val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                closeButton.setOnClickListener(View.OnClickListener { v: View? -> mediaView.performClick() })
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        closeButton.setOnClickListener(View.OnClickListener { v: View? ->
                            callback?.onResultFromActivityFull()
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

    var count: Int = 0

    override fun onResume() {
        super.onResume()
        count++
        if (count >= 2) {
            callback?.onResultFromActivityFull()
            finish()
        }
    }

    companion object {
        const val NATIVE_FUll_AD_ID_HIGH: String = "native_full_ad_id_high"

        const val NATIVE_FUll_AD_ID: String = "native_full_ad_id"

        private var callback: ActivityFullCallback? = null

        fun open(context: Context, high: String?, low: String?, cb: ActivityFullCallback?) {
            callback = cb
            val intent = Intent(context, ActivityLoadNativeFullV5::class.java)
            intent.putExtra(NATIVE_FUll_AD_ID_HIGH, high)
            intent.putExtra(NATIVE_FUll_AD_ID, low)
            context.startActivity(intent)
        }
    }
}

