package com.example.mycycle

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.mycycle.R
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
                "mycycle_reminders",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
