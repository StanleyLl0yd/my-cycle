package com.example.mycycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.engine.CyclePhaseCalculator
import com.example.mycycle.domain.engine.PredictionEngine
import com.example.mycycle.domain.model.CyclePhase
import com.example.mycycle.domain.model.Prediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayState(
    val cycleDay: Int? = null,
    val phase: CyclePhase? = null,
    val daysUntilPeriod: Int? = null,
    val daysUntilFertile: Int? = null,
    val isFertileNow: Boolean = false,
    val isPeriodToday: Boolean = false,
    val prediction: Prediction? = null,
    val isLoading: Boolean = true
)

class TodayViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine,
    private val phaseCalculator: CyclePhaseCalculator
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
                cycleDayRepository.observeAllPeriodDays()
            ) { preferences, periodDays ->
                Pair(preferences, periodDays)
            }.collect { (preferences, periodDays) ->
                val today = LocalDate.now()

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

                val cycleDay = lastPeriodStart?.let {
                    phaseCalculator.getCycleDay(today, it)
                }

                val phase = if (cycleDay != null) {
                    phaseCalculator.getPhase(
                        cycleDay = cycleDay,
                        cycleLength = preferences.estimatedCycleLength,
                        periodLength = preferences.estimatedPeriodLength
                    )
                } else null

                val daysUntilPeriod = prediction?.let {
                    phaseCalculator.getDaysUntilPeriod(today, it.nextPeriod.start)
                }

                val daysUntilFertile = prediction?.let {
                    phaseCalculator.getDaysUntilFertileWindow(today, it.fertileWindow.start)
                }

                val isFertileNow = prediction?.fertileWindow?.contains(today) == true
                val isPeriodToday = periodDays.any { it.date == today && it.hasPeriod }

                _state.update {
                    TodayState(
                        cycleDay = cycleDay,
                        phase = phase,
                        daysUntilPeriod = daysUntilPeriod,
                        daysUntilFertile = daysUntilFertile,
                        isFertileNow = isFertileNow,
                        isPeriodToday = isPeriodToday,
                        prediction = prediction,
                        isLoading = false
                    )
                }
            }
        }
    }
}
