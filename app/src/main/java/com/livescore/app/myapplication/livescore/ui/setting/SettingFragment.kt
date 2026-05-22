package com.livescore.app.myapplication.livescore.ui.setting

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.livescore.app.myapplication.livescore.databinding.FragmentSettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private var isVietnamese = true
    private var currentCacheSizeMb = 24.8

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
        updateCacheSizeUI()
    }

    private fun setupListeners() {
        // Push notifications switch
        binding.switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) {
                "Đã bật thông báo đẩy cập nhật bàn thắng thời gian thực!"
            } else {
                "Đã tắt thông báo đẩy."
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Dark mode switch
        binding.switchDarkMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                // Keep it checked to maintain the premium dark obsidian look
                buttonView.isChecked = true
                Toast.makeText(
                    requireContext(),
                    "Chế độ Obsidian Dark Mode mặc định được giữ để bảo vệ thị lực và tiết kiệm pin.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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

        // Clear cache
        binding.rowClearCache.setOnClickListener {
            if (currentCacheSizeMb == 0.0) {
                Toast.makeText(requireContext(), "Bộ nhớ đệm đã sạch!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.tvCacheSize.text = "Đang dọn dẹp..."
            
            // Post delay simulation
            Handler(Looper.getMainLooper()).postDelayed({
                val clearedSize = currentCacheSizeMb
                currentCacheSizeMb = 0.0
                updateCacheSizeUI()
                Toast.makeText(
                    requireContext(),
                    "Đã dọn dẹp thành công $clearedSize MB bộ nhớ đệm!",
                    Toast.LENGTH_SHORT
                ).show()
            }, 1000)
        }
    }

    private fun updateCacheSizeUI() {
        if (currentCacheSizeMb == 0.0) {
            binding.tvCacheSize.text = "0.0 KB"
        } else {
            binding.tvCacheSize.text = String.format("%.1f MB", currentCacheSizeMb)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
