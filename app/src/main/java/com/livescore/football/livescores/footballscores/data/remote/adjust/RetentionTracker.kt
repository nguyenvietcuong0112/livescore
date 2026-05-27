package com.livescore.football.livescores.footballscores.data.remote.adjust

import android.content.Context
import android.util.Log
import com.mallegan.ads.util.AdjustHelper

object RetentionTracker {

    private const val TAG = "RetentionTracker"
    private const val PREFS_NAME = "retention_prefs"

    fun checkAndTrackRetention(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            val diffInMs = System.currentTimeMillis() - installTime
            val diffInDays = (diffInMs / (1000 * 60 * 60 * 24)).toInt() + 1 // Day 1 = Install Day

            Log.d(TAG, "Calculating user retention: installDay = $diffInDays")

            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasTrackedToday = sharedPrefs.getBoolean("tracked_day_$diffInDays", false)

            if (!hasTrackedToday) {
                val token = when (diffInDays) {
                    1 -> AppAdjustTokens.RETENTION_DAY_1
                    3 -> AppAdjustTokens.RETENTION_DAY_3
                    7 -> AppAdjustTokens.RETENTION_DAY_7
                    else -> null
                }

                if (token != null) {
                    AdjustHelper.trackSimpleEvent(token)
                    sharedPrefs.edit().putBoolean("tracked_day_$diffInDays", true).apply()
                    Log.d(TAG, "Sent Adjust retention event for Day $diffInDays using token: $token")
                } else {
                    Log.d(TAG, "No retention event token configured for Day $diffInDays.")
                }
            } else {
                Log.d(TAG, "Adjust retention event for Day $diffInDays has already been tracked.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to track user retention via Adjust", e)
        }
    }
}
