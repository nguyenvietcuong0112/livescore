package com.livescore.football.livescores.footballscores.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.livescore.football.livescores.footballscores.BuildConfig
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.databinding.ActivityProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : BaseActivity() {

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private lateinit var binding: ActivityProfileBinding
    private var currentLangCode: String? = null

    override fun bind() {
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLangCode = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(this)

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

        val newLang = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(this)
        if (newLang.isNotEmpty() && currentLangCode != null && newLang != currentLangCode) {
            currentLangCode = newLang
            recreate()
            return
        }
        currentLangCode = newLang
        updateLanguageDisplay()
    }

    private fun displayAppVersion() {
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun updateLanguageDisplay() {
        val langCode = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(this)
        val langEnum = com.livescore.football.livescores.footballscores.utils.EnumSelectLanguage.entries.find { it.code.equals(langCode, ignoreCase = true) }
        val nameRes = langEnum?.nameLanguage ?: com.livescore.football.livescores.footballscores.R.string.language_english
        binding.tvLanguageValue.setText(nameRes)
    }

    private fun setupListeners() {
        updateLanguageDisplay()

        binding.rowLanguage.setOnClickListener {
            val intent = Intent(this, com.cscmobi.libraryads.views.language.CSCLanguageActivity::class.java).apply {
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.FROM_SETTING, true)
                putExtra("from_setting", true)
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.NAME_AD_NATIVE_LANGUAGE, "native_language")
                putExtra("name_ad_native_language", "native_language")
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
