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

    init {
        loadData()
    }

    fun previousMonth() {
        _state.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _state.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun goToToday() {
        _state.update { it.copy(currentMonth = YearMonth.now()) }
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

                val dayStatesMap = mutableMapOf<LocalDate, DayState>()
                val daysMap = allDays.associateBy { it.date }

                // Build day states for a range of months
                val startMonth = YearMonth.now().minusMonths(12)
                val endMonth = YearMonth.now().plusMonths(3)

                var month = startMonth
                while (!month.isAfter(endMonth)) {
                    val daysInMonth = month.lengthOfMonth()
                    for (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        val cycleDay = lastPeriodStart?.let {
                            phaseCalculator.getCycleDay(date, it)
                        }?.takeIf { it > 0 }

                        val phase = if (cycleDay != null && cycleDay <= preferences.estimatedCycleLength) {
                            phaseCalculator.getPhase(
                                cycleDay = cycleDay,
                                cycleLength = preferences.estimatedCycleLength,
                                periodLength = preferences.estimatedPeriodLength
                            )
                        } else null

                        val existingDay = daysMap[date]
                        val periodState = getPeriodState(date, existingDay, prediction)
                        val fertilityState = getFertilityState(date, prediction)

                        dayStatesMap[date] = DayState(
                            date = date,
                            cycleDay = cycleDay,
                            phase = phase,
                            periodState = periodState,
                            fertilityState = fertilityState,
                            symptoms = existingDay?.symptoms ?: emptySet(),
                            mood = existingDay?.mood,
                            hasNotes = !existingDay?.notes.isNullOrBlank(),
                            isToday = date == today,
                            isCurrentMonth = true
                        )
                    }
                    month = month.plusMonths(1)
                }

                _state.update {
                    it.copy(
                        dayStates = dayStatesMap,
                        prediction = prediction,
                        isLoading = false
                    )
                }
            }
        }
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
