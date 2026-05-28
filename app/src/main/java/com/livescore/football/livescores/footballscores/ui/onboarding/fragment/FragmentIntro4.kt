package com.livescore.football.livescores.footballscores.ui.onboarding.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.AbsBaseFragment
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.FragmentIntro4Binding
import com.livescore.football.livescores.footballscores.ui.onboarding.IAPActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroActivity
import com.livescore.football.livescores.footballscores.ui.onboarding.PermissionActivity
import com.livescore.football.livescores.footballscores.utils.ActivityFullCallback
import com.livescore.football.livescores.footballscores.utils.ActivityLoadNativeFullV1.Companion.open
import com.mallegan.ads.callback.InterCallback
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentIntro4 : AbsBaseFragment<FragmentIntro4Binding?>() {
    @Inject
    lateinit var limitManager: RequestLimitManager

    override fun getLayout(): Int {
        return R.layout.fragment_intro4
    }

    override fun initView() {
        binding!!.txtNext.setOnClickListener(View.OnClickListener { v: View? -> navigateNextScreen() })
        loadAds()
    }

    private fun navigateNextScreen() {
        val onboardingPrefs = requireActivity().getSharedPreferences(
            "livescore_onboarding_prefs",
            Context.MODE_PRIVATE
        )
        val isCompleted = onboardingPrefs.getBoolean("onboarding_completed", false)

        val intent: Intent?
        if (isCompleted) {
            if (hasNotificationPermission()) {
                if (limitManager.isPremium()) {
                    intent = Intent(requireActivity(), MainActivity::class.java)
                } else {
                    intent = Intent(requireActivity(), IAPActivity::class.java).apply {
                        putExtra("FROM_ONBOARDING", true)
                    }
                }
            } else {
                intent = Intent(requireActivity(), PermissionActivity::class.java)
            }
        } else {
            intent = Intent(requireActivity(), IntroActivity::class.java)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun loadAds() {
        Admob.getInstance().loadNativeAd(
            requireActivity(),
            getString(R.string.native_onboarding_4),
            object : NativeCallback() {
                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    if (!isAdded) return
                    binding!!.frAds.removeAllViews()
                    binding!!.frAds.setVisibility(View.GONE)
                }

                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    if (!isAdded) return
                    val adView = LayoutInflater.from(requireActivity())
                        .inflate(R.layout.layout_native_media, null) as NativeAdView?

                    binding!!.frAds.removeAllViews()
                    binding!!.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                }
            })
    }

    override fun onPause() {
        super.onPause()
    }
}