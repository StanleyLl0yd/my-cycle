package com.example.mycycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.CyclePhaseCalculator
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
    private val predictionEngine: PredictionEngine,
    private val phaseCalculator: CyclePhaseCalculator
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
                val periodDays = allDays.filter { it.hasPeriod }
                val cycles = cycleDetector.detectCycles(periodDays)

                val prediction = if (cycles.isNotEmpty()) {
                    predictionEngine.predictFromHistory(
                        cycles = cycles,
                        fallbackCycleLength = preferences.estimatedCycleLength,
                        fallbackPeriodLength = preferences.estimatedPeriodLength
                    )
                } else if (preferences.initialPeriodDate != null) {
                    predictionEngine.predictFromOnboarding(
                        lastPeriodStart = preferences.initialPeriodDate,
                        cycleLength = preferences.estimatedCycleLength,
                        periodLength = preferences.estimatedPeriodLength
                    )
                } else null

                val lastPeriodStart = cycles.lastOrNull()?.startDate
                    ?: preferences.initialPeriodDate

                val effectiveCycleLength = if (lastPeriodStart != null && prediction != null) {
                    ChronoUnit.DAYS.between(lastPeriodStart, prediction.nextPeriod.start)
                        .toInt()
                        .takeIf { it > 0 }
                        ?: preferences.estimatedCycleLength
                } else {
                    preferences.estimatedCycleLength
                }
                val effectivePeriodLength = prediction?.nextPeriod?.lengthDays
                    ?: preferences.estimatedPeriodLength

                val daysMap = allDays.associateBy { it.date }
                val dayStatesMap = buildDayStates(
                    month = month,
                    today = today,
                    daysMap = daysMap,
                    lastPeriodStart = lastPeriodStart,
                    prediction = prediction,
                    cycleLength = effectiveCycleLength,
                    periodLength = effectivePeriodLength
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
        prediction: Prediction?,
        cycleLength: Int,
        periodLength: Int
    ): Map<LocalDate, DayState> {
        val result = mutableMapOf<LocalDate, DayState>()

        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val cycleDay = lastPeriodStart?.let {
                phaseCalculator.getCycleDay(date, it)
            }?.takeIf { it > 0 }

            val phase = if (cycleDay != null && cycleDay <= cycleLength) {
                phaseCalculator.getPhase(
                    cycleDay = cycleDay,
                    cycleLength = cycleLength,
                    periodLength = periodLength
                )
            } else null

            val existingDay = daysMap[date]
            result[date] = DayState(
                date = date,
                cycleDay = cycleDay,
                phase = phase,
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
        if (cycleDay?.hasPeriod == true) {
            return when (cycleDay.flowIntensity) {
                FlowIntensity.SPOTTING -> PeriodState.CONFIRMED_SPOTTING
                FlowIntensity.LIGHT -> PeriodState.CONFIRMED_LIGHT
                FlowIntensity.MEDIUM -> PeriodState.CONFIRMED_MEDIUM
                FlowIntensity.HEAVY -> PeriodState.CONFIRMED_HEAVY
                null -> PeriodState.CONFIRMED_MEDIUM
            }
        }

        if (prediction != null && date in prediction.nextPeriod) {
            return PeriodState.PREDICTED
        }

        return PeriodState.NONE
    }

    private fun getFertilityState(
        date: LocalDate,
        prediction: Prediction?
    ): FertilityState {
        if (prediction == null) return FertilityState.NONE

        if (date == prediction.ovulationDate) {
            return FertilityState.OVULATION_PREDICTED
        }

        if (date in prediction.fertileWindow) {
            return FertilityState.FERTILE_PREDICTED
        }

        return FertilityState.NONE
    }
}
