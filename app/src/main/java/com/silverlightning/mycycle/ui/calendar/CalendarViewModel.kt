package com.silverlightning.mycycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverlightning.mycycle.data.preferences.UserPreferencesRepository
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.engine.CycleDetector
import com.silverlightning.mycycle.domain.engine.PredictionEngine
import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.DayState
import com.silverlightning.mycycle.domain.model.FertilityState
import com.silverlightning.mycycle.domain.model.FlowIntensity
import com.silverlightning.mycycle.domain.model.PeriodState
import com.silverlightning.mycycle.domain.model.Prediction
import com.silverlightning.mycycle.domain.model.UserPreferences
import com.silverlightning.mycycle.util.currentDateFlow
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val dayStates: Map<LocalDate, DayState> = emptyMap(),
    val prediction: Prediction? = null,
    val isLoading: Boolean = true
)

class CalendarViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine,
    private val clock: Clock
) : ViewModel() {

    private val initialToday = LocalDate.now(clock)
    private val _state = MutableStateFlow(
        CalendarState(
            currentMonth = YearMonth.from(initialToday),
            today = initialToday
        )
    )
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val selectedMonth = MutableStateFlow(YearMonth.from(initialToday))

    init {
        loadData()
    }

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }

    fun goToToday() {
        selectedMonth.value = YearMonth.from(LocalDate.now(clock))
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                cycleDayRepository.observeAll(),
                selectedMonth,
                currentDateFlow(clock)
            ) { preferences, allDays, month, today ->
                CalendarInput(preferences, allDays, month, today)
            }.collect { input ->
                val previousToday = _state.value.today
                val previousCurrentMonth = YearMonth.from(previousToday)
                val actualCurrentMonth = YearMonth.from(input.today)
                if (
                    input.month == previousCurrentMonth &&
                    input.month != actualCurrentMonth
                ) {
                    selectedMonth.value = actualCurrentMonth
                    return@collect
                }

                val cycles = cycleDetector.detectCycles(input.allDays)

                val prediction = if (cycles.isNotEmpty()) {
                    predictionEngine.predictFromHistory(
                        cycles = cycles,
                        fallbackCycleLength = input.preferences.estimatedCycleLength,
                        fallbackPeriodLength = input.preferences.estimatedPeriodLength,
                        stage = input.preferences.cycleStage,
                        referenceDate = input.today
                    )
                } else if (input.preferences.initialPeriodDate != null) {
                    predictionEngine.predictFromOnboarding(
                        lastPeriodStart = input.preferences.initialPeriodDate,
                        cycleLength = input.preferences.estimatedCycleLength,
                        periodLength = input.preferences.estimatedPeriodLength,
                        stage = input.preferences.cycleStage
                    )
                } else {
                    null
                }

                val lastPeriodStart = cycles.lastOrNull()?.startDate
                    ?: input.preferences.initialPeriodDate

                val daysMap = input.allDays.associateBy { it.date }
                val dayStatesMap = buildDayStates(
                    month = input.month,
                    today = input.today,
                    daysMap = daysMap,
                    lastPeriodStart = lastPeriodStart,
                    prediction = prediction
                )

                _state.update {
                    CalendarState(
                        currentMonth = input.month,
                        today = input.today,
                        dayStates = dayStatesMap,
                        prediction = prediction,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun buildDayStates(
        month: YearMonth,
        today: LocalDate,
        daysMap: Map<LocalDate, CycleDay>,
        lastPeriodStart: LocalDate?,
        prediction: Prediction?
    ): Map<LocalDate, DayState> {
        val result = mutableMapOf<LocalDate, DayState>()

        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val cycleDay = lastPeriodStart
                ?.let { ChronoUnit.DAYS.between(it, date).toInt() + 1 }
                ?.takeIf { it > 0 }
            val existingDay = daysMap[date]

            result[date] = DayState(
                date = date,
                cycleDay = cycleDay,
                periodState = getPeriodState(date, existingDay, prediction),
                fertilityState = getFertilityState(date, existingDay, prediction),
                symptoms = existingDay?.symptoms ?: emptySet(),
                mood = existingDay?.mood,
                hasNotes = !existingDay?.notes.isNullOrBlank(),
                isToday = date == today,
                isCurrentMonth = true
            )
        }

        return result
    }

    private fun getPeriodState(
        date: LocalDate,
        cycleDay: CycleDay?,
        prediction: Prediction?
    ): PeriodState {
        if (cycleDay?.flowIntensity == FlowIntensity.SPOTTING) {
            return PeriodState.CONFIRMED_SPOTTING
        }

        if (cycleDay?.isPeriodBleeding == true) {
            return when (cycleDay.flowIntensity) {
                FlowIntensity.LIGHT -> PeriodState.CONFIRMED_LIGHT
                FlowIntensity.MEDIUM -> PeriodState.CONFIRMED_MEDIUM
                FlowIntensity.HEAVY -> PeriodState.CONFIRMED_HEAVY
                FlowIntensity.SPOTTING -> PeriodState.CONFIRMED_SPOTTING
                null -> PeriodState.CONFIRMED_UNSPECIFIED
            }
        }

        if (cycleDay != null) {
            return PeriodState.NONE
        }

        if (prediction?.nextPeriodStartWindow?.contains(date) == true) {
            return PeriodState.PREDICTED
        }

        return PeriodState.NONE
    }

    private fun getFertilityState(
        date: LocalDate,
        cycleDay: CycleDay?,
        prediction: Prediction?
    ): FertilityState {
        if (prediction == null || cycleDay?.isPeriodBleeding == true) {
            return FertilityState.NONE
        }

        if (prediction.possibleOvulationWindow?.contains(date) == true) {
            return FertilityState.OVULATION_PREDICTED
        }

        if (prediction.possiblePregnancyWindow?.contains(date) == true) {
            return FertilityState.FERTILE_PREDICTED
        }

        return FertilityState.NONE
    }

    private data class CalendarInput(
        val preferences: UserPreferences,
        val allDays: List<CycleDay>,
        val month: YearMonth,
        val today: LocalDate
    )
}
