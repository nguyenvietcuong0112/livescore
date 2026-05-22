package com.livescore.app.myapplication.livescore

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.livescore.app.myapplication.livescore.data.local.RequestLimitManager
import com.livescore.app.myapplication.livescore.databinding.ActivityMainBinding
import com.livescore.app.myapplication.livescore.ui.favorite.FavoriteFragment
import com.livescore.app.myapplication.livescore.ui.home.HomeFragment
import com.livescore.app.myapplication.livescore.ui.leagues.LeaguesFragment
import com.livescore.app.myapplication.livescore.ui.search.SearchActivity
import com.livescore.app.myapplication.livescore.ui.setting.SettingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityMainBinding
    private var isLimitDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar action
        binding.searchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Set default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.newInstance(false))
                .commit()
        }

        // Setup bottom navigation selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment.newInstance(false))
                        .commit()
                    true
                }
                R.id.nav_live -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment.newInstance(true))
                        .commit()
                    true
                }
                R.id.nav_leagues -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LeaguesFragment())
                        .commit()
                    true
                }
                R.id.nav_favorite -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, FavoriteFragment())
                        .commit()
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Observe request limit exceeds
        lifecycleScope.launch {
            limitManager.limitExceededFlow.collect {
                if (!isLimitDialogShowing) {
                    showPremiumUpgradeDialog()
                }
            }
        }
    }

    private fun showPremiumUpgradeDialog() {
        isLimitDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle("⚡ Hạn mức hôm nay đã hết!")
            .setMessage("Bạn đã sử dụng hết 30 lượt truy cập miễn phí trong ngày hôm nay.\n\nHãy nâng cấp lên gói Premium (chỉ $1.99) để mở khóa trọn vẹn diễn biến trực tiếp, số liệu thống kê chuyên sâu & sơ đồ chiến thuật không giới hạn!")
            .setCancelable(false)
            .setPositiveButton("Nâng cấp Premium ($1.99)") { dialog, _ ->
                limitManager.setPremium(true)
                Toast.makeText(this, "Chúc mừng! Bạn đã nâng cấp thành công gói Premium không giới hạn. 🎉", Toast.LENGTH_LONG).show()
                isLimitDialogShowing = false
                dialog.dismiss()

                // Force reload current active fragment to show full stats under Premium status
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment != null) {
                    supportFragmentManager.beginTransaction()
                        .detach(currentFragment)
                        .attach(currentFragment)
                        .commitAllowingStateLoss()
                }
            }
            .setNegativeButton("Để sau") { dialog, _ ->
                isLimitDialogShowing = false
                dialog.dismiss()
            }
            .show()
    }
}
