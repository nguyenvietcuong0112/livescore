package com.livescore.football.livescores.footballscores.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.utils.SystemConfiguration
import com.livescore.football.livescores.footballscores.utils.SystemUtil


abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemConfiguration.setStatusBarColor(
            this,
            R.color.transparent,
            SystemConfiguration.IconColor.ICON_DARK
        )
        super.onCreate(savedInstanceState)
        SystemUtil.setLocale(this)
        bind()
    }

    abstract fun bind()
}
