package com.example.mycycle.di

import androidx.room.Room
import com.example.mycycle.data.local.AppDatabase
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.preferences.userPreferencesDataStore
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.CycleNoticeEvaluator
import com.example.mycycle.domain.engine.PredictionEngine
import com.example.mycycle.ui.calendar.CalendarViewModel
import com.example.mycycle.ui.daydetails.DayDetailsViewModel
import com.example.mycycle.ui.onboarding.OnboardingViewModel
import com.example.mycycle.ui.settings.SettingsViewModel
import com.example.mycycle.ui.statistics.StatisticsViewModel
import com.example.mycycle.ui.today.TodayViewModel
import java.time.Clock
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

    single { get<AppDatabase>().cycleDayDao() }
    single { androidContext().userPreferencesDataStore }
    single { Clock.systemDefaultZone() }

    single { UserPreferencesRepository(get()) }
    single { CycleDayRepository(get()) }

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
            clock = get()
        )
    }

    viewModel {
        CalendarViewModel(
            preferencesRepository = get(),
            cycleDayRepository = get(),
            cycleDetector = get(),
            predictionEngine = get(),
            clock = get()
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
