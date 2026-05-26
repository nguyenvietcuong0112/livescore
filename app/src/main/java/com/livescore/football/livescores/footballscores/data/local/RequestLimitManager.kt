package com.livescore.football.livescores.footballscores.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestLimitManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("livescore_request_limits_prefs", Context.MODE_PRIVATE)

    private val _limitExceededFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val limitExceededFlow: SharedFlow<Unit> = _limitExceededFlow.asSharedFlow()

    companion object {
        private const val KEY_LAST_DATE = "last_request_date"
        private const val KEY_REQ_COUNT = "request_count"
        private const val KEY_IS_PREMIUM = "is_premium_user"
        private const val DAILY_LIMIT = 20
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    @Synchronized
    fun isLimitExceeded(): Boolean {
        if (isPremium()) return false

        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")

        if (lastDate != today) {
            // New day, reset the counter
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_REQ_COUNT, 0)
                .apply()
            return false
        }

        val count = prefs.getInt(KEY_REQ_COUNT, 0)
        return count >= DAILY_LIMIT
    }

    @Synchronized
    fun incrementCount() {
        if (isPremium()) return

        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")

        val currentCount = if (lastDate == today) {
            prefs.getInt(KEY_REQ_COUNT, 0)
        } else {
            0
        }

        prefs.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_REQ_COUNT, currentCount + 1)
            .apply()
    }

    fun triggerLimitExceeded() {
        _limitExceededFlow.tryEmit(Unit)
    }

    fun getRemainingRequests(): Int {
        if (isPremium()) return 999999
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val count = if (lastDate == today) prefs.getInt(KEY_REQ_COUNT, 0) else 0
        return (DAILY_LIMIT - count).coerceAtLeast(0)
    }

    fun isNearQuotaLimit(): Boolean {
        if (isPremium()) return false
        return getRemainingRequests() <= 5
    }

    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremium(isPremium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        // Instantly notify observers of quota changes when premium is acquired
        triggerLimitExceeded() 
    }
}
