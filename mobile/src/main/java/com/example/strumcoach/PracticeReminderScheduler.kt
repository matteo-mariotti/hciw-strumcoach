package com.example.strumcoach

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "practice_reminder"

object PracticeReminderScheduler {

    fun schedule(context: Context, hourOfDay: Int) {
        val request = PeriodicWorkRequestBuilder<PracticeReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(hourOfDay), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    // WorkManager periodic work isn't an exact alarm — this only sets a starting point
    // close to the desired hour so the daily cadence lands near it, not a guarantee.
    private fun delayUntilNext(hourOfDay: Int): Long {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var target = now.toLocalDate().atTime(LocalTime.of(hourOfDay, 0))
        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        return java.time.Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}
