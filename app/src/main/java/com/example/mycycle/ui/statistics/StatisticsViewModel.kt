package com.example.mycycle.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.CycleStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CycleRegularity {
    REGULAR,
    SOMEWHAT_REGULAR,
    IRREGULAR
}

data class StatisticsState(
    val averageCycleLength: Int? = null,
    val averagePeriodLength: Int? = null,
    val cycleVariationDays: Int? = null,
    val regularity: CycleRegularity? = null,
    val completedCycleCount: Int = 0,
    val cycles: List<Cycle> = emptyList(),
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val isLoading: Boolean = true
)

class StatisticsViewModel(
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

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
                val lengths = completed.mapNotNull { it.length }
                val enoughForAverages = completed.size >= 2

                val averageCycleLength = lengths
                    .takeIf { enoughForAverages }
                    ?.average()
                    ?.roundToInt()

                val averagePeriodLength = completed
                    .takeIf { enoughForAverages }
                    ?.map { it.periodLength }
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

                _state.update {
                    StatisticsState(
                        averageCycleLength = averageCycleLength,
                        averagePeriodLength = averagePeriodLength,
                        cycleVariationDays = variation,
                        regularity = regularity,
                        completedCycleCount = completed.size,
                        cycles = cycles.takeLast(12).reversed(),
                        cycleStage = preferences.cycleStage,
                        isLoading = false
                    )
                }
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
            variationDays <= 9 -> CycleRegularity.REGULAR
            variationDays <= 14 -> CycleRegularity.SOMEWHAT_REGULAR
            else -> CycleRegularity.IRREGULAR
        }
    }
}
