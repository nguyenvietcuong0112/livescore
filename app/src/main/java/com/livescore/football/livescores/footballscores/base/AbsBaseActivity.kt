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
}
