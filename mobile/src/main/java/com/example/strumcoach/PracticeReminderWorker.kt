package com.example.strumcoach

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.ZoneId

private const val CHANNEL_ID = "practice_reminders"
private const val NOTIFICATION_ID = 1001

class PracticeReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("strum_coach_data", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("reminder_enabled", false)) {
            return Result.success()
        }

        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val lastPracticeDay = prefs.getLong("last_practice_epoch_day", -1L)
        if (lastPracticeDay == today) {
            // Already practiced today — a reminder here would just be nagging.
            return Result.success()
        }

        val streak = prefs.getInt("current_streak_snapshot", 0)
        showNotification(streak)
        return Result.success()
    }

    private fun showNotification(streak: Int) {
        val context = applicationContext
        createChannelIfNeeded(context)

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (streak > 0) {
            "Don't lose your $streak-day streak — pick up the guitar for a quick session."
        } else {
            "Ready to strum? A few minutes of practice keeps your rhythm sharp."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_strumcoach_2)
            .setContentTitle("Time to practice")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Practice reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminder to practice your strumming"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
