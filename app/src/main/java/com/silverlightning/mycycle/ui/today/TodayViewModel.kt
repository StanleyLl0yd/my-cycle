package com.silverlightning.mycycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverlightning.mycycle.data.preferences.UserPreferencesRepository
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.engine.CycleDetector
import com.silverlightning.mycycle.domain.engine.CycleNoticeEvaluator
import com.silverlightning.mycycle.domain.engine.PredictionEngine
import com.silverlightning.mycycle.domain.model.CycleNotice
import com.silverlightning.mycycle.domain.model.CycleStage
import com.silverlightning.mycycle.domain.model.Prediction
import com.silverlightning.mycycle.util.currentDateFlow
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayState(
    val today: LocalDate = LocalDate.now(),
    val cycleDay: Int? = null,
    val isPeriodToday: Boolean = false,
    val prediction: Prediction? = null,
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val notice: CycleNotice? = null,
    val isLoading: Boolean = true
)

class TodayViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val predictionEngine: PredictionEngine,
    private val noticeEvaluator: CycleNoticeEvaluator,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(
        TodayState(today = LocalDate.now(clock))
    )
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                cycleDayRepository.observeAll(),
                currentDateFlow(clock)
            ) { preferences, allDays, today ->
                Triple(preferences, allDays, today)
            }.collect { (preferences, allDays, today) ->
                val cycles = cycleDetector.detectCycles(allDays)

                val prediction = if (cycles.isNotEmpty()) {
                    predictionEngine.predictFromHistory(
                        cycles = cycles,
                        fallbackCycleLength = preferences.estimatedCycleLength,
                        fallbackPeriodLength = preferences.estimatedPeriodLength,
                        stage = preferences.cycleStage,
                        referenceDate = today
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
                    ?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 }
                    ?.takeIf { it > 0 }

                val todayEntry = allDays.firstOrNull { it.date == today }
                val isPeriodToday = todayEntry?.isPeriodBleeding == true
                val bleedingToday = todayEntry?.flowIntensity != null || isPeriodToday

                val latestCompletedLength = cycles
                    .lastOrNull { it.isComplete }
                    ?.length
                val currentPeriodLength = cycles.lastOrNull()
                    ?.takeIf { !it.isComplete }
                    ?.periodLength

                val notice = noticeEvaluator.evaluate(
                    stage = preferences.cycleStage,
                    latestCompletedLength = latestCompletedLength,
                    currentCycleDay = cycleDay,
                    currentPeriodLength = currentPeriodLength,
                    isPeriodToday = isPeriodToday,
                    bleedingToday = bleedingToday
                )

                _state.update {
                    TodayState(
                        today = today,
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
}
