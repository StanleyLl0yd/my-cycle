package com.example.mycycle.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mycycle.data.R
import com.example.mycycle.data.local.entity.ReminderEntity
import com.example.mycycle.model.ReminderType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

internal class ReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun updateSchedule(reminder: ReminderEntity) {
        workManager.cancelUniqueWork(workName(reminder.id))
        if (reminder.enabled) {
            enqueue(reminder)
        }
    }

    fun cancel(reminderId: String) {
        workManager.cancelUniqueWork(workName(reminderId))
    }

    fun rescheduleAll(reminders: List<ReminderEntity>) {
        reminders.forEach { reminder ->
            workManager.cancelUniqueWork(workName(reminder.id))
            if (reminder.enabled) enqueue(reminder)
        }
    }

    private fun enqueue(reminder: ReminderEntity) {
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

        workManager.enqueueUniqueWork(workName(reminder.id), ExistingWorkPolicy.REPLACE, request)
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
        val channelId = context.getString(R.string.reminder_channel_id)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                context.getString(R.string.reminder_channel_name),
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
        val text = when (type) {
            ReminderType.CYCLE_START -> context.getString(R.string.reminder_body_cycle_start)
            ReminderType.OVULATION -> context.getString(R.string.reminder_body_ovulation)
            ReminderType.MEDICATION -> context.getString(R.string.reminder_body_medication)
            ReminderType.GENERAL -> context.getString(R.string.reminder_body_general)
        }
        val targetRoute = when (type) {
            ReminderType.CYCLE_START, ReminderType.OVULATION -> "calendar"
            ReminderType.MEDICATION, ReminderType.GENERAL -> "reminders"
        }
        val launchIntent = Intent().apply {
            setClassName(context.packageName, "com.example.mycycle.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(KEY_TARGET_DESTINATION, targetRoute)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(reminderId.hashCode(), notification)
    }

    internal fun scheduleNext(reminderId: String, type: ReminderType, time: LocalTime) {
        val reminder = ReminderEntity(
            id = reminderId,
            type = type.name,
            time = time,
            enabled = true
        )
        enqueue(reminder)
    }

    private fun workName(id: String): String = "reminder_$id"

    companion object {
        internal const val KEY_REMINDER_ID = "reminder_id"
        internal const val KEY_REMINDER_TYPE = "reminder_type"
        internal const val KEY_REMINDER_TIME = "reminder_time"
        internal const val KEY_TARGET_DESTINATION = "target_destination"
    }
}
