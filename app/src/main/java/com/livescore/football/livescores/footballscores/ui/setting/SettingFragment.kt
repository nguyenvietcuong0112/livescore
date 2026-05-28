package com.livescore.football.livescores.footballscores.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.livescore.football.livescores.footballscores.databinding.FragmentSettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private var isVietnamese = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayAppVersion()
        setupListeners()
    }

    private fun displayAppVersion() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = "v$versionName"
        } catch (e: Exception) {
            binding.tvVersion.text = "v1.0.0"
        }
    }

    private fun setupListeners() {
        // Language toggle
        binding.rowLanguage.setOnClickListener {
            isVietnamese = !isVietnamese
            if (isVietnamese) {
                binding.tvLanguageValue.text = "Tiếng Việt 🇻🇳"
                Toast.makeText(requireContext(), getString(com.livescore.football.livescores.footballscores.R.string.toast_language_switched_vi), Toast.LENGTH_SHORT).show()
            } else {
                binding.tvLanguageValue.text = "English 🇺🇸"
                Toast.makeText(requireContext(), getString(com.livescore.football.livescores.footballscores.R.string.toast_language_switched_en), Toast.LENGTH_SHORT).show()
            }
        }

        // Privacy Policy link
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

