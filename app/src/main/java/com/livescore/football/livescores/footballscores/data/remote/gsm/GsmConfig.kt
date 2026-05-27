package com.livescore.football.livescores.footballscores.data.remote.gsm

import com.livescore.football.livescores.footballscores.BuildConfig

object GsmConfig {
    const val PROD_BASE_URL = "https://gsm.cscmobicorp.com/"
    const val DEV_BASE_URL = "https://gsmdev.cscmobicorp.com/"
    
    // Dynamically routes to Developer Sandbox URL in Debug, and Production URL in Release
    val CURRENT_BASE_URL: String
        get() = if (BuildConfig.DEBUG) DEV_BASE_URL else PROD_BASE_URL
    
    // Default App ID as specified
    const val GSM_APP_ID = "6a1417f03f16a04e36ed6dea"
}
