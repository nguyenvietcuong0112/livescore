package com.livescore.football.livescores.footballscores.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.ActivityPermissionBinding
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityPermissionBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        binding.switchPermission.isChecked = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set initial state
        binding.switchPermission.isChecked = hasNotificationPermission()

        binding.switchPermission.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) { // Only handle user clicks, not programmatic changes
                if (isChecked && !hasNotificationPermission()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else if (!isChecked && hasNotificationPermission()) {
                    // Cannot easily revoke via app, visual only
                    // Typically you might launch settings, but visual is fine for now
                }
            }
        }

        binding.btnSkip.setOnClickListener {
            if (binding.frSkip.isClickable) {
                navigateToHome()
            }
        }
        binding.frSkip.setOnClickListener {
            if (binding.frSkip.isClickable) {
                navigateToHome()
            }
        }

        loadAds()
    }

    private fun enableSkip() {
        binding.icLoading.visibility = android.view.View.GONE
        binding.btnSkip.visibility = android.view.View.VISIBLE
        binding.frSkip.isClickable = true
        binding.frSkip.isFocusable = true
        binding.btnSkip.isClickable = true
        binding.btnSkip.isFocusable = true
    }

    private fun loadAds() {
        val adId = getString(R.string.native_permission)
        if (adId.isNotEmpty()) {
            Admob.getInstance().loadNativeAds(this, adId, 1, object : com.mallegan.ads.callback.NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: com.google.android.gms.ads.nativead.NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    val adView = android.view.LayoutInflater.from(this@PermissionActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    com.mallegan.ads.util.Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    enableSkip()
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    binding.frAds.removeAllViews()
                    binding.frAds.visibility = android.view.View.GONE
                    enableSkip()
                }
            })
        } else {
            binding.frAds.removeAllViews()
            binding.frAds.visibility = android.view.View.GONE
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
