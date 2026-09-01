package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.DateRange
import com.sl.mycycle.domain.model.Prediction
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PredictionEngine {

    companion object {
        private const val FERTILE_BEFORE_OVULATION = 5
        private const val LUTEAL_PHASE_MIN_DAYS = 11
        private const val LUTEAL_PHASE_MAX_DAYS = 17
        private const val MAX_CYCLES_FOR_AVERAGE = 6
        private const val MIN_CYCLES_FOR_OVULATION_ESTIMATE = 3
        private const val DEFAULT_CYCLE_LENGTH = 28
        private const val DEFAULT_WINDOW_RADIUS = 14
        private const val FIRST_YEAR_WINDOW_RADIUS = 10
        private const val EARLY_YEARS_WINDOW_RADIUS = 7
        private const val ESTABLISHED_WINDOW_RADIUS = 3
        private const val EARLY_YEARS_VARIABILITY_DAYS = 14
        private const val ESTABLISHED_VARIABILITY_DAYS = 9
        private val EARLY_YEARS_COMMON_RANGE = 21..45
        private val ESTABLISHED_COMMON_RANGE = 21..35
    }

    fun predictFromOnboarding(
        lastPeriodStart: LocalDate,
        cycleLength: Int,
        stage: CycleStage = CycleStage.NOT_SET
    ): Prediction {
        if (stage == CycleStage.PERIODS_STOPPED) {
            return stoppedPrediction(stage)
        }

        return calculatePrediction(
            lastPeriodStart = lastPeriodStart,
            avgCycleLength = cycleLength,
            cycleCount = 0,
            stdDev = 0f,
            spread = 0,
            stage = stage
        )
    }

    fun predictFromHistory(
        cycles: List<Cycle>,
        fallbackCycleLength: Int,
        referenceDate: LocalDate,
        stage: CycleStage = CycleStage.NOT_SET
    ): Prediction {
        if (stage == CycleStage.PERIODS_STOPPED) {
            return stoppedPrediction(stage)
        }

        val completeCycles = cycles.filter { it.isComplete }

        if (completeCycles.isEmpty()) {
            val lastCycle = cycles.lastOrNull()
            return predictFromOnboarding(
                lastPeriodStart = lastCycle?.startDate ?: referenceDate,
                cycleLength = fallbackCycleLength,
                stage = stage
            )
        }

        val recentCycles = completeCycles.takeLast(MAX_CYCLES_FOR_AVERAGE)
        val lengths = recentCycles.mapNotNull { it.length }
        val avgCycleLength = weightedAverage(lengths)
        val stdDev = standardDeviation(lengths)
        val spread = if (lengths.isEmpty()) 0 else lengths.max() - lengths.min()

        return calculatePrediction(
            lastPeriodStart = cycles.last().startDate,
            avgCycleLength = avgCycleLength,
            cycleCount = recentCycles.size,
            stdDev = stdDev,
            spread = spread,
            stage = stage
        )
    }

    private fun calculatePrediction(
        lastPeriodStart: LocalDate,
        avgCycleLength: Int,
        cycleCount: Int,
        stdDev: Float,
        spread: Int,
        stage: CycleStage
    ): Prediction {
        val highlyVariable = isHighlyVariable(stage, spread)
        val outsideCommonRange = isOutsideCommonRange(stage, avgCycleLength)
        val historyRadius = maxOf(
            ceil(spread / 2.0).toInt(),
            ceil(stdDev * 1.5f).toInt()
        )
        val radius = maxOf(
            minimumWindowRadius(stage),
            historyRadius,
            if (outsideCommonRange) EARLY_YEARS_WINDOW_RADIUS else 0
        )
        val centerDate = lastPeriodStart.plusDays(avgCycleLength.toLong())
        val nextPeriodStartWindow = DateRange(
            start = centerDate.minusDays(radius.toLong()),
            end = centerDate.plusDays(radius.toLong())
        )

        val canEstimateOvulation =
            stage == CycleStage.ESTABLISHED &&
                cycleCount >= MIN_CYCLES_FOR_OVULATION_ESTIMATE &&
                !highlyVariable &&
                !outsideCommonRange

        val possibleOvulationWindow = if (canEstimateOvulation) {
            DateRange(
                start = nextPeriodStartWindow.start.minusDays(LUTEAL_PHASE_MAX_DAYS.toLong()),
                end = nextPeriodStartWindow.end.minusDays(LUTEAL_PHASE_MIN_DAYS.toLong())
            )
        } else {
            null
        }

        val possiblePregnancyWindow = possibleOvulationWindow?.let {
            DateRange(
                start = it.start.minusDays(FERTILE_BEFORE_OVULATION.toLong()),
                end = it.end
            )
        }

        return Prediction(
            nextPeriodStartWindow = nextPeriodStartWindow,
            possiblePregnancyWindow = possiblePregnancyWindow,
            possibleOvulationWindow = possibleOvulationWindow,
            basedOnCycles = cycleCount,
            highlyVariable = highlyVariable,
            outsideCommonRange = outsideCommonRange,
            stage = stage
        )
    }

    private fun stoppedPrediction(stage: CycleStage): Prediction = Prediction(
        nextPeriodStartWindow = null,
        possiblePregnancyWindow = null,
        possibleOvulationWindow = null,
        basedOnCycles = 0,
        highlyVariable = true,
        outsideCommonRange = false,
        stage = stage
    )

    private fun minimumWindowRadius(stage: CycleStage): Int = when (stage) {
        CycleStage.NOT_SET -> DEFAULT_WINDOW_RADIUS
        CycleStage.FIRST_YEAR -> FIRST_YEAR_WINDOW_RADIUS
        CycleStage.YEARS_ONE_TO_THREE -> EARLY_YEARS_WINDOW_RADIUS
        CycleStage.ESTABLISHED -> ESTABLISHED_WINDOW_RADIUS
        CycleStage.LONG_TERM_UNEVEN -> DEFAULT_WINDOW_RADIUS
        CycleStage.CHANGING_WITH_AGE -> DEFAULT_WINDOW_RADIUS
        CycleStage.PERIODS_STOPPED -> 0
    }

    private fun isHighlyVariable(
        stage: CycleStage,
        spread: Int
    ): Boolean = when (stage) {
        CycleStage.NOT_SET -> true
        CycleStage.FIRST_YEAR -> true
        CycleStage.YEARS_ONE_TO_THREE -> spread > EARLY_YEARS_VARIABILITY_DAYS
        CycleStage.ESTABLISHED -> spread > ESTABLISHED_VARIABILITY_DAYS
        CycleStage.LONG_TERM_UNEVEN -> true
        CycleStage.CHANGING_WITH_AGE -> true
        CycleStage.PERIODS_STOPPED -> true
    }

    private fun isOutsideCommonRange(
        stage: CycleStage,
        averageLength: Int
    ): Boolean = when (stage) {
        CycleStage.YEARS_ONE_TO_THREE -> averageLength !in EARLY_YEARS_COMMON_RANGE
        CycleStage.ESTABLISHED -> averageLength !in ESTABLISHED_COMMON_RANGE
        else -> false
    }

    private fun weightedAverage(values: List<Int>): Int {
        if (values.isEmpty()) return DEFAULT_CYCLE_LENGTH
        if (values.size == 1) return values.first()

        var weightedSum = 0.0
        var weightSum = 0.0

        values.forEachIndexed { index, value ->
            val weight = index + 1.0
            weightedSum += value * weight
            weightSum += weight
        }

        return (weightedSum / weightSum).roundToInt()
    }

    private fun standardDeviation(values: List<Int>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
}
