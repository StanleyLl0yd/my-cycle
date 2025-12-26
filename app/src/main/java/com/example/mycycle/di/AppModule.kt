package com.example.mycycle.di

import androidx.room.Room
import com.example.mycycle.data.local.AppDatabase
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.preferences.userPreferencesDataStore
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.CyclePhaseCalculator
import com.example.mycycle.domain.engine.PredictionEngine
import com.example.mycycle.ui.calendar.CalendarViewModel
import com.example.mycycle.ui.daydetails.DayDetailsViewModel
import com.example.mycycle.ui.onboarding.OnboardingViewModel
import com.example.mycycle.ui.settings.SettingsViewModel
import com.example.mycycle.ui.today.TodayViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mycycle.db"
        ).build()
    }

    single { get<AppDatabase>().cycleDayDao() }

    // DataStore
    single { androidContext().userPreferencesDataStore }

    // Repositories
    single { UserPreferencesRepository(get()) }
    single { CycleDayRepository(get()) }

    // Domain engines
    single { CycleDetector() }
    single { PredictionEngine() }
    single { CyclePhaseCalculator() }

    // ViewModels
    viewModel { OnboardingViewModel(get(), get()) }

    viewModel {
        TodayViewModel(
            preferencesRepository = get(),
            cycleDayRepository = get(),
            cycleDetector = get(),
            predictionEngine = get(),
            phaseCalculator = get()
        )
    }

    viewModel {
        CalendarViewModel(
            preferencesRepository = get(),
            cycleDayRepository = get(),
            cycleDetector = get(),
            predictionEngine = get(),
            phaseCalculator = get()
        )
    }

    viewModel { SettingsViewModel(get()) }

    viewModel { (dateString: String) ->
        DayDetailsViewModel(dateString, get())
    }
}
