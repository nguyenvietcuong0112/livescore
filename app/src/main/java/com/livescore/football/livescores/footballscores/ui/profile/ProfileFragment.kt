package com.livescore.football.livescores.footballscores.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.livescore.football.livescores.footballscores.BuildConfig
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentLangCode: String? = null

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Profile"
        )
        val newLang = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(requireContext())
        if (newLang.isNotEmpty() && currentLangCode != null && newLang != currentLangCode) {
            currentLangCode = newLang
            requireActivity().recreate()
            return
        }
        currentLangCode = newLang
        updateLanguageDisplay()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentLangCode = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(requireContext())
        setupListeners()
        displayAppVersion()

    }


    private fun displayAppVersion() {
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun updateLanguageDisplay() {
        val langCode = com.livescore.football.livescores.footballscores.utils.SystemUtil.getPreLanguage(requireContext())
        val langEnum = com.livescore.football.livescores.footballscores.utils.EnumSelectLanguage.entries.find { it.code.equals(langCode, ignoreCase = true) }
        val nameRes = langEnum?.nameLanguage ?: R.string.language_english
        binding.tvLanguageValue.setText(nameRes)
    }

    private fun setupListeners() {
        updateLanguageDisplay()

        // Language selection: Navigate to CSCLanguageActivity
        binding.rowLanguage.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.cscmobi.libraryads.views.language.CSCLanguageActivity::class.java).apply {
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.FROM_SETTING, true)
                putExtra("from_setting", true)
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.NAME_AD_NATIVE_LANGUAGE, "native_language")
                putExtra("name_ad_native_language", "native_language")
            }
            startActivity(intent)
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://sites.google.com/view/apfolife-privacy-policy/")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
