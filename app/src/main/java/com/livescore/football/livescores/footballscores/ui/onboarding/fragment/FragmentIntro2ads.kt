package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.databinding.FragmentAdsBinding
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob


class FragmentIntro2ads : AbsBaseFragment<FragmentAdsBinding?>() {
    var viewPager: ViewPager2? = null

    override fun getLayout(): Int {
        return R.layout.fragment_ads
    }

    override fun initView() {
        viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        loadNativeFull(getString(R.string.native_onboarding_full_1))
    }

    private fun loadNativeFull(adId: String?) {
        Admob.getInstance().loadNativeAds(requireActivity(), adId, 1, object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                binding!!.frAdsFull.setVisibility(View.GONE)
                binding!!.animLoading.setVisibility(View.VISIBLE)
            }

            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                val adView = LayoutInflater.from(requireActivity())
                    .inflate(R.layout.native_full_language, null) as NativeAdView
                val closeButton = adView.findViewById<ImageView>(R.id.close)
                val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                closeButton.setOnClickListener(View.OnClickListener { v: View? -> mediaView.performClick() })
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        closeButton.setOnClickListener(View.OnClickListener { v: View? ->
                            viewPager!!.setCurrentItem(3)
                        })
                    }
                }.start()
                binding!!.frAdsFull.removeAllViews()
                binding!!.frAdsFull.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
            }
        })
    }
}
