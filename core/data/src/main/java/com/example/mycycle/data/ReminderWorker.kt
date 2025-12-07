package com.example.mycycle.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mycycle.data.local.DatabaseProvider
import com.example.mycycle.model.ReminderType
import java.time.LocalTime
import kotlinx.coroutines.flow.first

internal class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(ReminderScheduler.KEY_REMINDER_ID) ?: return Result.failure()
        val type = inputData.getString(ReminderScheduler.KEY_REMINDER_TYPE)?.let { ReminderType.valueOf(it) }
            ?: return Result.failure()
        val storedReminder = DatabaseProvider.create(applicationContext).reminderDao().observeReminders().first()
            .firstOrNull { it.id == reminderId }
        val time = storedReminder?.time
            ?: inputData.getString(ReminderScheduler.KEY_REMINDER_TIME)?.let { LocalTime.parse(it) }
            ?: return Result.failure()

        ReminderScheduler(applicationContext).notify(reminderId, type)
        if (storedReminder?.enabled != false) {
            ReminderScheduler(applicationContext).scheduleNext(reminderId, type, time)
        }
        return Result.success()
    }
}
