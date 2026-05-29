package com.livescore.football.livescores.footballscores.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.utils.SystemConfiguration


abstract class AbsBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_DARK
        )
        bind()
    }

    abstract fun bind()

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }
    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = getWindow()
            window.setDecorFitsSystemWindows(false)
            val insetsController = window.getInsetsController()
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.navigationBars())
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
            }
        } else {
            val decorView = getWindow().getDecorView()
            decorView.setSystemUiVisibility(
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            )
        }
    }
}
