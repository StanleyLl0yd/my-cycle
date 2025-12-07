package com.example.mycycle.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mycycle.model.ReminderType

internal class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(ReminderScheduler.KEY_REMINDER_ID) ?: return Result.failure()
        val type = inputData.getString(ReminderScheduler.KEY_REMINDER_TYPE)?.let { ReminderType.valueOf(it) }
            ?: return Result.failure()

        ReminderScheduler(applicationContext).notify(reminderId, type)
        return Result.success()
    }
}
