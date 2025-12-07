package com.example.mycycle.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mycycle.model.ReminderType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

internal class ReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun updateSchedule(reminder: com.example.mycycle.data.local.entity.ReminderEntity) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(computeDelay(reminder.time), TimeUnit.MILLISECONDS)
            .addTag(reminder.id)
            .setInputData(
                workDataOf(
                    KEY_REMINDER_ID to reminder.id,
                    KEY_REMINDER_TYPE to reminder.type,
                    KEY_REMINDER_TIME to reminder.time.toString(),
                )
            )
            .build()

        workManager.cancelAllWorkByTag(reminder.id)
        if (reminder.enabled) {
            workManager.enqueue(request)
        }
    }

    private fun computeDelay(time: LocalTime): Long {
        val now = LocalTime.now()
        val today = LocalDate.now()
        val targetDate = if (now.isBefore(time)) today else today.plusDays(1)
        val targetDateTime = targetDate.atTime(time)
        val millis = Duration.between(java.time.LocalDateTime.now(), targetDateTime).toMillis()
        return millis.coerceAtLeast(0)
    }

    internal fun notify(reminderId: String, type: ReminderType) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val channelId = "mycycle_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cycle reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val text = when (type) {
            ReminderType.CYCLE_START -> "Пора отметить начало цикла"
            ReminderType.OVULATION -> "Напоминание об овуляции"
            ReminderType.MEDICATION -> "Время принять препараты"
            ReminderType.GENERAL -> "Напоминание из календаря"
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("My Cycle")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(reminderId.hashCode(), notification)
    }

    companion object {
        internal const val KEY_REMINDER_ID = "reminder_id"
        internal const val KEY_REMINDER_TYPE = "reminder_type"
        internal const val KEY_REMINDER_TIME = "reminder_time"
    }
}
