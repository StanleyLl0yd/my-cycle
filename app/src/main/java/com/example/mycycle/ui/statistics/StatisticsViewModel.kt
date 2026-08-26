package com.example.mycycle.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.engine.CycleDetector
import com.example.mycycle.domain.model.Cycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class CycleRegularity {
    REGULAR,
    SOMEWHAT_REGULAR,
    IRREGULAR
}

data class StatisticsState(
    val averageCycleLength: Int? = null,
    val averagePeriodLength: Int? = null,
    val regularity: CycleRegularity? = null,
    val completedCycleCount: Int = 0,
    val cycles: List<Cycle> = emptyList(),
    val isLoading: Boolean = true
)

class StatisticsViewModel(
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cycleDayRepository.observeAllPeriodDays().collect { periodDays ->
                val cycles = cycleDetector.detectCycles(periodDays)
                val completed = cycles.filter { it.isComplete && it.length != null }
                val lengths = completed.mapNotNull { it.length }

                val averageCycleLength = lengths
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.roundToInt()

                val averagePeriodLength = completed
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.periodLength }
                    ?.average()
                    ?.roundToInt()

                val regularity = lengths
                    .takeIf { it.size >= 2 }
                    ?.let { classifyRegularity(standardDeviation(it)) }

                _state.update {
                    StatisticsState(
                        averageCycleLength = averageCycleLength,
                        averagePeriodLength = averagePeriodLength,
                        regularity = regularity,
                        completedCycleCount = completed.size,
                        cycles = cycles.takeLast(12).reversed(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun classifyRegularity(stdDev: Float): CycleRegularity = when {
        stdDev <= 2f -> CycleRegularity.REGULAR
        stdDev <= 5f -> CycleRegularity.SOMEWHAT_REGULAR
        else -> CycleRegularity.IRREGULAR
    }

    private fun standardDeviation(values: List<Int>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
}
