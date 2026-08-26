package com.example.mycycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.PredictionEngine
import com.example.mycycle.domain.model.CycleStage
import com.example.mycycle.domain.model.Prediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TodayNotice {
    CYCLE_STAGE_NOT_SET,
    FIRST_YEAR_CHANGES_ARE_COMMON,
    EARLY_YEARS_CHANGES_ARE_COMMON,
    LONG_TERM_UNEVEN,
    CHANGING_WITH_AGE,
    PERIODS_STOPPED,
    THREE_MONTH_GAP,
    BLEEDING_AFTER_YEAR_GAP,
    LONG_BLEEDING,
    OUTSIDE_COMMON_RANGE
}

data class TodayState(
    val cycleDay: Int? = null,
    val isPeriodToday: Boolean = false,
    val prediction: Prediction? = null,
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val notice: TodayNotice? = null,
    val isLoading: Boolean = true
)

class TodayViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine
) : ViewModel() {

    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                cycleDayRepository.observeAll()
            ) { preferences, allDays ->
                Pair(preferences, allDays)
            }.collect { (preferences, allDays) ->
                val today = LocalDate.now()
                val cycles = cycleDetector.detectCycles(allDays)

                val prediction = if (cycles.isNotEmpty()) {
                    predictionEngine.predictFromHistory(
                        cycles = cycles,
                        fallbackCycleLength = preferences.estimatedCycleLength,
                        fallbackPeriodLength = preferences.estimatedPeriodLength,
                        stage = preferences.cycleStage
                    )
                } else if (preferences.initialPeriodDate != null) {
                    predictionEngine.predictFromOnboarding(
                        lastPeriodStart = preferences.initialPeriodDate,
                        cycleLength = preferences.estimatedCycleLength,
                        periodLength = preferences.estimatedPeriodLength,
                        stage = preferences.cycleStage
                    )
                } else if (preferences.cycleStage == CycleStage.PERIODS_STOPPED) {
                    predictionEngine.predictFromOnboarding(
                        lastPeriodStart = today,
                        cycleLength = preferences.estimatedCycleLength,
                        periodLength = preferences.estimatedPeriodLength,
                        stage = preferences.cycleStage
                    )
                } else {
                    null
                }

                val lastPeriodStart = cycles.lastOrNull()?.startDate
                    ?: preferences.initialPeriodDate

                val cycleDay = lastPeriodStart
                    ?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() + 1 }
                    ?.takeIf { it > 0 }

                val todayEntry = allDays.firstOrNull { it.date == today }
                val isPeriodToday = todayEntry?.hasPeriod == true
                val bleedingToday = todayEntry?.flowIntensity != null || isPeriodToday

                val completedLengths = cycles
                    .filter { it.isComplete }
                    .mapNotNull { it.length }
                val latestCompletedLength = completedLengths.lastOrNull()
                val currentPeriodLength = cycles.lastOrNull()
                    ?.takeIf { !it.isComplete }
                    ?.periodLength

                val notice = chooseNotice(
                    stage = preferences.cycleStage,
                    completedLengths = completedLengths,
                    latestCompletedLength = latestCompletedLength,
                    currentCycleDay = cycleDay,
                    currentPeriodLength = currentPeriodLength,
                    isPeriodToday = isPeriodToday,
                    bleedingToday = bleedingToday
                )

                _state.update {
                    TodayState(
                        cycleDay = cycleDay,
                        isPeriodToday = isPeriodToday,
                        prediction = prediction,
                        cycleStage = preferences.cycleStage,
                        notice = notice,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun chooseNotice(
        stage: CycleStage,
        completedLengths: List<Int>,
        latestCompletedLength: Int?,
        currentCycleDay: Int?,
        currentPeriodLength: Int?,
        isPeriodToday: Boolean,
        bleedingToday: Boolean
    ): TodayNotice? {
        if (
            (stage == CycleStage.PERIODS_STOPPED && bleedingToday) ||
            latestCompletedLength?.let { it >= 365 } == true
        ) {
            return TodayNotice.BLEEDING_AFTER_YEAR_GAP
        }

        val longBleedingLimit = when (stage) {
            CycleStage.FIRST_YEAR,
            CycleStage.YEARS_ONE_TO_THREE -> 7
            else -> 8
        }
        if (
            isPeriodToday &&
            currentPeriodLength != null &&
            currentPeriodLength > longBleedingLimit
        ) {
            return TodayNotice.LONG_BLEEDING
        }

        if (
            stage in setOf(CycleStage.FIRST_YEAR, CycleStage.YEARS_ONE_TO_THREE) &&
            (completedLengths.any { it >= 90 } || (currentCycleDay ?: 0) >= 90)
        ) {
            return TodayNotice.THREE_MONTH_GAP
        }

        val outsideCommonRange = when (stage) {
            CycleStage.YEARS_ONE_TO_THREE ->
                latestCompletedLength?.let { it !in 21..45 } == true ||
                    (currentCycleDay ?: 0) > 45
            CycleStage.ESTABLISHED,
            CycleStage.LONG_TERM_UNEVEN ->
                latestCompletedLength?.let { it !in 21..35 } == true ||
                    (currentCycleDay ?: 0) > 35
            else -> false
        }
        if (outsideCommonRange) {
            return TodayNotice.OUTSIDE_COMMON_RANGE
        }

        return when (stage) {
            CycleStage.NOT_SET -> TodayNotice.CYCLE_STAGE_NOT_SET
            CycleStage.FIRST_YEAR -> TodayNotice.FIRST_YEAR_CHANGES_ARE_COMMON
            CycleStage.YEARS_ONE_TO_THREE -> TodayNotice.EARLY_YEARS_CHANGES_ARE_COMMON
            CycleStage.ESTABLISHED -> null
            CycleStage.LONG_TERM_UNEVEN -> TodayNotice.LONG_TERM_UNEVEN
            CycleStage.CHANGING_WITH_AGE -> TodayNotice.CHANGING_WITH_AGE
            CycleStage.PERIODS_STOPPED -> TodayNotice.PERIODS_STOPPED
        }
    }
}
