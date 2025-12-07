package com.example.mycycle

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.mycycle.data.R as DataR
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyCycleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(DataR.string.reminder_channel_id),
                getString(DataR.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(DataR.string.reminder_channel_description)
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
