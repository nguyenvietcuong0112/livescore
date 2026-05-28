package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.view.LayoutInflater
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.databinding.FragmentIntro1Binding
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.mallegan.ads.util.Admob

class FragmentIntro1 : AbsBaseFragment<FragmentIntro1Binding?>() {
    override fun getLayout(): Int {
        return R.layout.fragment_intro1
    }


    override fun initView() {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        binding!!.txtNext.setOnClickListener(View.OnClickListener { view: View? ->
            viewPager.setCurrentItem(
                1
            )
        })
        loadAdsIntro1()
    }

    private fun loadAdsIntro1() {
        if (AdsConfig.nativeIntro1 != null) {
            val adView = LayoutInflater.from(requireActivity())
                .inflate(R.layout.layout_native_media, null) as NativeAdView?

            binding!!.frAds.removeAllViews()
            binding!!.frAds.addView(adView)
            Admob.getInstance().pushAdsToViewCustom(AdsConfig.nativeIntro1, adView)
        } else {
            goneAds()
        }
    }

    private fun goneAds() {
        binding!!.frAds.removeAllViews()
        binding!!.frAds.setVisibility(View.GONE)
    }
}
