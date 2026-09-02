package com.sl.mycycle.di

import androidx.room.Room
import com.sl.mycycle.data.local.AppDatabase
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.preferences.userPreferencesDataStore
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.data.transfer.DataPortabilityService
import com.sl.mycycle.domain.engine.CycleDetector
import com.sl.mycycle.domain.engine.CycleNoticeEvaluator
import com.sl.mycycle.domain.engine.PredictionEngine
import com.sl.mycycle.reminder.ReminderScheduler
import com.sl.mycycle.ui.calendar.CalendarViewModel
import com.sl.mycycle.ui.daydetails.DayDetailsViewModel
import com.sl.mycycle.ui.onboarding.OnboardingViewModel
import com.sl.mycycle.ui.settings.SettingsViewModel
import com.sl.mycycle.ui.statistics.StatisticsViewModel
import com.sl.mycycle.ui.today.TodayViewModel
import com.sl.mycycle.util.ClockProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mycycle.db"
        ).build()
    }

    single { androidContext().userPreferencesDataStore }
    single { ClockProvider() }

    single { UserPreferencesRepository(get()) }
    single { CycleDayRepository(get()) }
    single { DataPortabilityService(get(), get()) }
    single { ReminderScheduler(androidContext()) }

    single { CycleDetector() }
    single { PredictionEngine() }
    single { CycleNoticeEvaluator() }

    viewModel { OnboardingViewModel(get(), get(), get()) }

    viewModel {
        TodayViewModel(
            preferencesRepository = get(),
            cycleDayRepository = get(),
            cycleDetector = get(),
            predictionEngine = get(),
            noticeEvaluator = get(),
            clockProvider = get()
        )
    }

    viewModel {
        CalendarViewModel(
            preferencesRepository = get(),
            cycleDayRepository = get(),
            cycleDetector = get(),
            predictionEngine = get(),
            clockProvider = get()
        )
    }

    viewModel {
        StatisticsViewModel(
            cycleDayRepository = get(),
            cycleDetector = get(),
            preferencesRepository = get()
        )
    }

    viewModel { SettingsViewModel(get(), get()) }

    viewModel { (dateString: String) ->
        DayDetailsViewModel(dateString, get(), get())
    }
}
