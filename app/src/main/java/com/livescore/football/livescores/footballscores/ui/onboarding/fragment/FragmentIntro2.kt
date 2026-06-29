package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.view.LayoutInflater
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.data.remote.getRemoteAdId
import com.livescore.football.livescores.footballscores.databinding.FragmentIntro2Binding
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroSlideshowActivity
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV1
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.livescore.football.livescores.footballscores.utils.SharePreferenceUtils
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentIntro2 : AbsBaseFragment<FragmentIntro2Binding?>() {
    
    @Inject
    lateinit var limitManager: com.livescore.football.livescores.footballscores.data.local.RequestLimitManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    var isAdLoading: Boolean = false

    override fun getLayout(): Int {
        return R.layout.fragment_intro2
    }

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Onboarding2"
        )
    }

    override fun initView() {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        binding!!.txtNext.setOnClickListener(View.OnClickListener { view: View? ->
            val nextItem = if (::limitManager.isInitialized && limitManager.isPremium()) 3 else 2
            viewPager.setCurrentItem(nextItem)
        })
        if (!SharePreferenceUtils.isOrganic(context)) {
            loadAds()
        } else {
            binding!!.frAds.visibility = View.GONE
        }
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

    private fun loadAds() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding!!.frAds.visibility = View.GONE
            return
        }

        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_banner_ob", getString(R.string.native_banner_ob))
        } catch (e: Exception) {
            getString(R.string.native_banner_ob)
        }
        if (adId.isNotEmpty()) {
            isAdLoading = true
            (activity as? IntroSlideshowActivity)?.updateSwipeState()
            showLoadingNext(true)
            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                liveScoreApiService, requireContext(), "native", adId, "Onboarding2"
            )
            Admob.getInstance().loadNativeAd(
                requireActivity(),
                adId,
                object : NativeCallback() {
                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        isAdLoading = false
                        (activity as? IntroSlideshowActivity)?.updateSwipeState()
                        if (!isAdded) return
                        showLoadingNext(false)
                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                            liveScoreApiService, requireContext(), "native", adId, "Onboarding2", null
                        )
                        binding!!.frAds.removeAllViews()
                        binding!!.frAds.visibility = View.GONE
                    }

                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        if (!isAdded) {
                            isAdLoading = false
                            return
                        }

                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                            liveScoreApiService, requireContext(), "native", adId, "Onboarding2"
                        )

                        nativeAd?.setOnPaidEventListener { adValue ->
                            val ecpm = adValue.valueMicros / 1000.0
                            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                                liveScoreApiService, requireContext(), "native", adId, "Onboarding2", ecpm
                            )
                        }

                        val adView = LayoutInflater.from(requireActivity())
                            .inflate(R.layout.layout_native_no_media, null) as NativeAdView

                        binding!!.frAds.removeAllViews()
                        binding!!.frAds.addView(adView)
                        Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                        context?.let { LogEvent.log(it, "native_banner_ob") }

                        binding!!.frAds.postDelayed({
                            isAdLoading = false
                            (activity as? IntroSlideshowActivity)?.updateSwipeState()
                            showLoadingNext(false)
                        }, 500)
                    }
                }
            )
        } else {
            binding!!.frAds.visibility = View.GONE
        }
    }
}
