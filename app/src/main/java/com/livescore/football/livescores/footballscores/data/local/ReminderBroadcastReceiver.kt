package com.livescore.football.livescores.footballscores.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.livescore.football.livescores.footballscores.MainActivity
import com.livescore.football.livescores.footballscores.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getIntExtra("MATCH_ID", 0)
        val matchTitle = intent.getStringExtra("MATCH_TITLE") ?: "Trận đấu sắp diễn ra"
        val leagueName = intent.getStringExtra("LEAGUE_NAME") ?: "Giải đấu"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "match_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Match Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for match reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, matchId, openIntent, flags)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_live) // Reusing existing white system icon
            .setContentTitle("⚽ Trận đấu sắp bắt đầu!")
            .setContentText("$matchTitle ($leagueName) sẽ khởi tranh sau 5 phút nữa. Đừng bỏ lỡ!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(matchId, notification)
    }
}
