package com.sl.mycycle.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.engine.CycleDetector
import com.sl.mycycle.domain.engine.InsightEngine
import com.sl.mycycle.domain.engine.SymptomInsight
import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleStage
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class CycleRegularity {
    REGULAR,
    SOMEWHAT_REGULAR,
    IRREGULAR
}

data class StatisticsState(
    val averageCycleLength: Int? = null,
    val averagePeriodLength: Int? = null,
    val shortestCycleLength: Int? = null,
    val longestCycleLength: Int? = null,
    val cycleVariationDays: Int? = null,
    val regularity: CycleRegularity? = null,
    val completedCycleCount: Int = 0,
    val trackedDayCount: Int = 0,
    val symptomInsights: List<SymptomInsight> = emptyList(),
    val cycles: List<Cycle> = emptyList(),
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val isLoading: Boolean = true
)

class StatisticsViewModel(
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val preferencesRepository: UserPreferencesRepository,
    private val insightEngine: InsightEngine
) : ViewModel() {

    companion object {
        private const val MAX_CYCLES_FOR_SUMMARY = 6
        private const val MAX_CYCLES_TO_DISPLAY = 12
        private const val REGULAR_VARIATION_DAYS = 9
        private const val SOMEWHAT_REGULAR_VARIATION_DAYS = 14
    }

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cycleDayRepository.observeAll(),
                preferencesRepository.preferences
            ) { allDays, preferences ->
                Pair(allDays, preferences)
            }.collect { (allDays, preferences) ->
                val cycles = cycleDetector.detectCycles(allDays)
                val completed = cycles.filter { it.isComplete && it.length != null }
                val recent = completed.takeLast(MAX_CYCLES_FOR_SUMMARY)
                val lengths = recent.mapNotNull { it.length }
                val periodLengths = recent.mapNotNull { it.periodLength }
                val insights = insightEngine.analyze(allDays, cycles)

                val averageCycleLength = lengths
                    .takeIf { it.size >= 2 }
                    ?.average()
                    ?.roundToInt()

                val averagePeriodLength = periodLengths
                    .takeIf { it.size >= 2 }
                    ?.average()
                    ?.roundToInt()

                val variation = lengths
                    .takeIf { it.size >= 2 }
                    ?.let { it.max() - it.min() }

                val regularity = variation?.let {
                    classifyChange(
                        variationDays = it,
                        stage = preferences.cycleStage
                    )
                }

                _state.value = StatisticsState(
                    averageCycleLength = averageCycleLength,
                    averagePeriodLength = averagePeriodLength,
                    shortestCycleLength = insights.shortestCycleLength,
                    longestCycleLength = insights.longestCycleLength,
                    cycleVariationDays = variation,
                    regularity = regularity,
                    completedCycleCount = recent.size,
                    trackedDayCount = insights.trackedDayCount,
                    symptomInsights = insights.symptomInsights,
                    cycles = cycles.takeLast(MAX_CYCLES_TO_DISPLAY).reversed(),
                    cycleStage = preferences.cycleStage,
                    isLoading = false
                )
            }
        }
    }

    private fun classifyChange(
        variationDays: Int,
        stage: CycleStage
    ): CycleRegularity? = when (stage) {
        CycleStage.NOT_SET,
        CycleStage.FIRST_YEAR,
        CycleStage.YEARS_ONE_TO_THREE,
        CycleStage.LONG_TERM_UNEVEN,
        CycleStage.CHANGING_WITH_AGE,
        CycleStage.PERIODS_STOPPED -> null

        CycleStage.ESTABLISHED -> when {
            variationDays <= REGULAR_VARIATION_DAYS -> CycleRegularity.REGULAR
            variationDays <= SOMEWHAT_REGULAR_VARIATION_DAYS -> CycleRegularity.SOMEWHAT_REGULAR
            else -> CycleRegularity.IRREGULAR
        }
    }
}
