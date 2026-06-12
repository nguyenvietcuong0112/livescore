package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.view.LayoutInflater
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.databinding.FragmentIntro1Binding
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentIntro1 : AbsBaseFragment<FragmentIntro1Binding?>() {

    @Inject
    lateinit var limitManager: com.livescore.football.livescores.footballscores.data.local.RequestLimitManager

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

    private fun showLoadingNext(isLoading: Boolean) {
        if (isLoading) {
            binding?.txtNext?.text = ""
            binding?.txtNext?.isClickable = false
            binding?.loadingNext?.visibility = View.VISIBLE
        } else {
            binding?.txtNext?.text = getString(R.string.intro_next)
            binding?.txtNext?.isClickable = true
            binding?.loadingNext?.visibility = View.GONE
        }
    }

    private fun loadAdsIntro1() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            goneAds()
            return
        }

        if (AdsConfig.nativeIntro1 != null) {
            val adView = LayoutInflater.from(requireActivity())
                .inflate(R.layout.layout_native_media, null) as NativeAdView?

            binding!!.frAds.removeAllViews()
            binding!!.frAds.addView(adView)
            Admob.getInstance().pushAdsToViewCustom(AdsConfig.nativeIntro1, adView)
            context?.let { LogEvent.log(it, "native_onboarding_1") }
        } else {
            loadAdsIntro1Dynamically()
        }
    }

    private fun loadAdsIntro1Dynamically() {
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_onboarding_1", getString(R.string.native_onboarding_1))
        } catch (e: Exception) {
            getString(R.string.native_onboarding_1)
        }

        if (adId.isNotEmpty()) {
            showLoadingNext(true)
            Admob.getInstance().loadNativeAd(
                requireActivity(),
                adId,
                object : NativeCallback() {
                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        if (!isAdded) return
                        showLoadingNext(false)
                        goneAds()
                    }

                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        if (!isAdded) return
                        AdsConfig.nativeIntro1 = nativeAd
                        
                        val adView = LayoutInflater.from(requireActivity())
                            .inflate(R.layout.layout_native_media, null) as NativeAdView?

                        binding!!.frAds.removeAllViews()
                        binding!!.frAds.addView(adView)
                        if (nativeAd != null && adView != null) {
                            Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                        }
                        context?.let { LogEvent.log(it, "native_onboarding_1") }
                        binding!!.frAds.postDelayed({
                            showLoadingNext(false)
                        }, 500)
                    }
                }
            )
        } else {
            goneAds()
        }
    }

    private fun goneAds() {
        binding!!.frAds.removeAllViews()
        binding!!.frAds.visibility = View.GONE
    }
}
