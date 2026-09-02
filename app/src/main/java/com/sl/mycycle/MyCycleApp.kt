package com.sl.mycycle

import android.app.Application
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.di.appModule
import com.sl.mycycle.reminder.ReminderNotifier
import com.sl.mycycle.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MyCycleApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        val koin = startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MyCycleApp)
            modules(appModule)
        }.koin

        ReminderNotifier.createChannel(this)
        appScope.launch {
            val preferences = koin.get<UserPreferencesRepository>().preferences.first()
            koin.get<ReminderScheduler>().sync(
                preferences.dailyReminderEnabled,
                preferences.reminderHour,
                preferences.reminderMinute
            )
        }
    }
}
