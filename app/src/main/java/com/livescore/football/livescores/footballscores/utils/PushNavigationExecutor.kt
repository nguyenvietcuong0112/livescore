package com.livescore.football.livescores.footballscores.utils

import android.content.Context
import android.content.Intent
import com.livescore.football.livescores.footballscores.ui.detail.MatchDetailActivity
import com.livescore.football.livescores.footballscores.ui.iap.IAPActivity
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import com.livescore.football.livescores.footballscores.ui.splash.SplashActivity

object PushNavigationExecutor {

    const val PROMO_VIP_SUBSCRIPTION = "vip_subscription"

    fun createSplashIntent(context: Context, payload: PushPayload): Intent {
        return Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            payload.rawData.forEach { (key, value) -> putExtra(key, value) }
        }
    }

    fun toDestinationIntent(context: Context, navigation: PushNavigation): Intent {
        return when (navigation) {
            is PushNavigation.Match -> Intent(context, MatchDetailActivity::class.java).apply {
                putExtra("MATCH_ID", navigation.fixtureId)
            }
            is PushNavigation.League -> Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_LEAGUE_ID, navigation.leagueId)
            }
            is PushNavigation.News -> Intent(context, MainActivity::class.java)
            is PushNavigation.Promo -> {
                if (navigation.screenName == PROMO_VIP_SUBSCRIPTION) {
                    Intent(context, IAPActivity::class.java).apply {
                        putExtra(IAPActivity.EXTRA_FROM_PUSH, true)
                    }
                } else {
                    Intent(context, MainActivity::class.java)
                }
            }
            PushNavigation.Default -> Intent(context, MainActivity::class.java)
        }
    }

    fun notificationId(payload: PushPayload): Int {
        return payload.pushId?.hashCode()
            ?: payload.navigation.hashCode()
    }
}
