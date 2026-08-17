package com.livescore.football.livescores.footballscores.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.livescore.football.livescores.footballscores.BuildConfig
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityProfileBinding
import com.livescore.football.livescores.footballscores.ui.language.LanguageActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : BaseActivity() {

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private lateinit var binding: ActivityProfileBinding

    override fun bind() {
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        displayAppVersion()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Profile"
        )
        updateLanguageDisplay()
    }

    private fun displayAppVersion() {
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun updateLanguageDisplay() {
        val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
        val selectedLanguage = onboardingPrefs.getString("selected_language", "English") ?: "English"
        binding.tvLanguageValue.text = selectedLanguage
    }

    private fun setupListeners() {
        updateLanguageDisplay()

        binding.rowLanguage.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java).apply {
                putExtra(LanguageActivity.EXTRA_FROM_PROFILE, true)
            }
            startActivity(intent)
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://sites.google.com/view/apfolife-privacy-policy/")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
