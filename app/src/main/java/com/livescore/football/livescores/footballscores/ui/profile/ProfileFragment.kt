package com.livescore.football.livescores.footballscores.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var isVietnamese = true

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
        setupListeners()
    }

    private fun setupListeners() {
        val onboardingPrefs = requireContext().getSharedPreferences("livescore_onboarding_prefs", android.content.Context.MODE_PRIVATE)
        val selectedLanguage = onboardingPrefs.getString("selected_language", "English") ?: "English"
        binding.tvLanguageValue.text = selectedLanguage

        // Language toggle
        binding.rowLanguage.setOnClickListener {
            val languages = arrayOf(
                "Arabic", "English", "French", "German", "Hindi",
                "Indonesian", "Italian", "Japanese", "Portuguese", "Russian",
                "Spanish", "Thai", "Turkish", "Urdu", "Vietnamese"
            )
            val currentIdx = languages.indexOf(binding.tvLanguageValue.text.toString()).coerceAtLeast(0)
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.language_dialog_title))
                .setSingleChoiceItems(languages, currentIdx) { dialog, which ->
                    val newLang = languages[which]
                    onboardingPrefs.edit().putString("selected_language", newLang).apply()
                    binding.tvLanguageValue.text = newLang
                    
                    // Apply dynamic system locale instantly
                    val localeCode = when (newLang) {
                        "Arabic" -> "ar"
                        "English" -> "en"
                        "French" -> "fr"
                        "German" -> "de"
                        "Hindi" -> "hi"
                        "Indonesian" -> "id"
                        "Italian" -> "it"
                        "Japanese" -> "ja"
                        "Portuguese" -> "pt"
                        "Russian" -> "ru"
                        "Spanish" -> "es"
                        "Thai" -> "th"
                        "Turkish" -> "tr"
                        "Urdu" -> "ur"
                        "Vietnamese" -> "vi"
                        else -> "en"
                    }
                    val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(localeCode)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)

                    Toast.makeText(requireContext(), getString(R.string.language_changed_toast, newLang), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
