package com.sl.mycycle.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.preferences.userPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            ReminderScheduler.ACTION_REMINDER,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = UserPreferencesRepository(context.userPreferencesDataStore)
                    .preferences
                    .first()
                val scheduler = ReminderScheduler(context)

                if (
                    action == ReminderScheduler.ACTION_REMINDER &&
                    preferences.dailyReminderEnabled
                ) {
                    ReminderNotifier.show(context)
                }

                scheduler.sync(
                    preferences.dailyReminderEnabled,
                    preferences.reminderHour,
                    preferences.reminderMinute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
