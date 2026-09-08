package com.sl.mycycle.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.engine.CycleDetector
import com.sl.mycycle.domain.engine.PredictionEngine
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.Prediction
import com.sl.mycycle.domain.model.UserPreferences
import com.sl.mycycle.util.ClockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

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
                val koin = GlobalContext.get()
                val preferences = koin.get<UserPreferencesRepository>()
                    .preferences
                    .first()
                val scheduler = koin.get<ReminderScheduler>()

                if (
                    action == ReminderScheduler.ACTION_REMINDER &&
                    preferences.dailyReminderEnabled
                ) {
                    ReminderNotifier.show(
                        context,
                        resolveReminderKind(koin, preferences)
                    )
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

    private suspend fun resolveReminderKind(
        koin: Koin,
        preferences: UserPreferences
    ): ReminderKind {
        val today = koin.get<ClockProvider>().today()
        val days = koin.get<CycleDayRepository>().observeAll().first()
        val todayEntry = days.firstOrNull { it.date == today }
        val cycles = koin.get<CycleDetector>().detectCycles(days)
        val prediction = prediction(koin, preferences, cycles.isNotEmpty(), today, cycles)

        return koin.get<ReminderDecisionEngine>().decide(
            today = today,
            todayEntry = todayEntry,
            prediction = prediction
        )
    }

    private fun prediction(
        koin: Koin,
        preferences: UserPreferences,
        hasCycles: Boolean,
        today: java.time.LocalDate,
        cycles: List<com.sl.mycycle.domain.model.Cycle>
    ): Prediction? {
        val engine = koin.get<PredictionEngine>()
        return when {
            hasCycles -> engine.predictFromHistory(
                cycles = cycles,
                fallbackCycleLength = preferences.estimatedCycleLength,
                fallbackPeriodLength = preferences.estimatedPeriodLength,
                referenceDate = today,
                stage = preferences.cycleStage
            )
            preferences.initialPeriodDate != null -> engine.predictFromOnboarding(
                lastPeriodStart = preferences.initialPeriodDate,
                cycleLength = preferences.estimatedCycleLength,
                periodLength = preferences.estimatedPeriodLength,
                stage = preferences.cycleStage
            )
            preferences.cycleStage == CycleStage.PERIODS_STOPPED -> engine.predictFromOnboarding(
                lastPeriodStart = today,
                cycleLength = preferences.estimatedCycleLength,
                periodLength = preferences.estimatedPeriodLength,
                stage = preferences.cycleStage
            )
            else -> null
        }
    }
}
