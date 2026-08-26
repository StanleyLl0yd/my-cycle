package com.example.mycycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.PredictionEngine
import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.DayState
import com.example.mycycle.domain.model.FertilityState
import com.example.mycycle.domain.model.FlowIntensity
import com.example.mycycle.domain.model.PeriodState
import com.example.mycycle.domain.model.Prediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val dayStates: Map<LocalDate, DayState> = emptyMap(),
    val prediction: Prediction? = null,
    val isLoading: Boolean = true
)

class CalendarViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val selectedMonth = MutableStateFlow(YearMonth.now())

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
        selectedMonth.value = YearMonth.now()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                cycleDayRepository.observeAll(),
                selectedMonth
            ) { preferences, allDays, month ->
                Triple(preferences, allDays, month)
            }.collect { (preferences, allDays, month) ->
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
                } else {
                    null
                }

                val lastPeriodStart = cycles.lastOrNull()?.startDate
                    ?: preferences.initialPeriodDate

                val daysMap = allDays.associateBy { it.date }
                val dayStatesMap = buildDayStates(
                    month = month,
                    today = today,
                    daysMap = daysMap,
                    lastPeriodStart = lastPeriodStart,
                    prediction = prediction
                )

                _state.update {
                    CalendarState(
                        currentMonth = month,
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
                phase = null,
                periodState = getPeriodState(date, existingDay, prediction),
                fertilityState = getFertilityState(date, prediction),
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

        if (cycleDay?.hasPeriod == true) {
            return when (cycleDay.flowIntensity) {
                FlowIntensity.LIGHT -> PeriodState.CONFIRMED_LIGHT
                FlowIntensity.MEDIUM -> PeriodState.CONFIRMED_MEDIUM
                FlowIntensity.HEAVY -> PeriodState.CONFIRMED_HEAVY
                FlowIntensity.SPOTTING -> PeriodState.CONFIRMED_SPOTTING
                null -> PeriodState.CONFIRMED_MEDIUM
            }
        }

        if (prediction?.nextPeriodStartWindow?.contains(date) == true) {
            return PeriodState.PREDICTED
        }

        return PeriodState.NONE
    }

    private fun getFertilityState(
        date: LocalDate,
        prediction: Prediction?
    ): FertilityState {
        if (prediction == null) return FertilityState.NONE

        if (prediction.possibleOvulationWindow?.contains(date) == true) {
            return FertilityState.OVULATION_PREDICTED
        }

        if (prediction.possiblePregnancyWindow?.contains(date) == true) {
            return FertilityState.FERTILE_PREDICTED
        }

        return FertilityState.NONE
    }
}
