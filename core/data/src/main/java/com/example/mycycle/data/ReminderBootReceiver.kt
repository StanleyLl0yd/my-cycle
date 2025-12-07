package com.example.mycycle.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mycycle.data.local.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

public class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = DatabaseProvider.create(context.applicationContext)
                val reminders = database.reminderDao().observeReminders().first()
                ReminderScheduler(context.applicationContext).rescheduleAll(reminders)
            }
        }
    }
}
