package com.livescore.football.livescores.footballscores.data.local

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("match_reminders_prefs", Context.MODE_PRIVATE)

    fun isReminderSet(matchId: Int): Boolean {
        return prefs.getBoolean(matchId.toString(), false)
    }

    fun toggleReminder(match: CachedMatchEntity): Boolean {
        val current = isReminderSet(match.id)
        val newState = !current
        prefs.edit().putBoolean(match.id.toString(), newState).apply()

        if (newState) {
            scheduleAlarm(match)
        } else {
            cancelAlarm(match)
        }
        return newState
    }

    private fun scheduleAlarm(match: CachedMatchEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("MATCH_ID", match.id)
            putExtra("MATCH_TITLE", "${match.homeTeamName} vs ${match.awayTeamName}")
            putExtra("LEAGUE_NAME", match.leagueName)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, match.id, intent, flags)

        // Schedule 5 minutes before match start time
        val triggerTimeMs = (match.dateTimestamp * 1000) - (5 * 60 * 1000)
        
        // If it starts in less than 5 minutes or already started, trigger in 5 seconds for visual testing
        val finalTriggerTimeMs = if (triggerTimeMs <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 5000
        } else {
            triggerTimeMs
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTimeMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, finalTriggerTimeMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalTriggerTimeMs, pendingIntent)
            }
            Log.d("MatchReminderManager", "Scheduled reminder alarm for match ${match.id} at $finalTriggerTimeMs")
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, finalTriggerTimeMs, pendingIntent)
        }
    }

    private fun cancelAlarm(match: CachedMatchEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, match.id, intent, flags)
        alarmManager.cancel(pendingIntent)
        Log.d("MatchReminderManager", "Cancelled reminder alarm for match ${match.id}")
    }

    fun getAllReminderIds(): Set<Int> {
        return prefs.all.filter { it.value as? Boolean == true }.keys.mapNotNull { it.toIntOrNull() }.toSet()
    }
}
