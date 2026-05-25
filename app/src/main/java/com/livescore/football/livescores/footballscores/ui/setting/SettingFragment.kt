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
        setupListeners()
    }

    private fun setupListeners() {
        // Language toggle
        binding.rowLanguage.setOnClickListener {
            isVietnamese = !isVietnamese
            if (isVietnamese) {
                binding.tvLanguageValue.text = "Tiếng Việt 🇻🇳"
                Toast.makeText(requireContext(), "Đã chuyển đổi sang Tiếng Việt", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvLanguageValue.text = "English 🇺🇸"
                Toast.makeText(requireContext(), "Switched to English", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

