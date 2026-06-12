package com.livescore.football.livescores.footballscores.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.databinding.ActivityPermissionBinding
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.ui.iap.IAPActivity
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : BaseActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityPermissionBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Direct navigation to home regardless of granted or denied permission state
        navigateToHome()
    }

    override fun bind() {
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        LogEvent.log(this, "question_view")

        // Request permission on Continue click, then proceed
        binding.btnContinue.setOnClickListener {
            if (!hasNotificationPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    navigateToHome()
                }
            } else {
                navigateToHome()
            }
        }

        // Direct skip to home
        binding.btnSkip.setOnClickListener {
            navigateToHome()
        }

        loadAds()
    }

    private fun enableSkip() {
        binding.btnSkip.visibility = View.VISIBLE
        binding.btnSkip.isClickable = true
        binding.btnSkip.isFocusable = true
    }

    private fun loadAds() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding.frAds.visibility = View.GONE
            enableSkip()
            return
        }
        val adId = try {
            RemoteConfigManager.Companion.getInstance()
                .getAdId("native_permission", getString(R.string.native_permission))
        } catch (e: Exception) {
            getString(R.string.native_permission)
        }
        if (adId.isNotEmpty()) {
            Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    binding.frAds.removeAllViews()
                    binding.frAds.visibility = View.GONE
                    enableSkip()
                }

                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    val adView = LayoutInflater.from(this@PermissionActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    LogEvent.log(this@PermissionActivity, "native_permission")
                    enableSkip()
                }
            })
        } else {
            binding.frAds.removeAllViews()
            binding.frAds.visibility = View.GONE
            enableSkip()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun navigateToHome() {
        val intent = if (limitManager.isPremium()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, IAPActivity::class.java).apply {
                putExtra("FROM_ONBOARDING", true)
            }
        }
        startActivity(intent)
        finish()
    }
}