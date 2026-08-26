package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.CycleStage
import com.example.mycycle.domain.model.DateRange
import com.example.mycycle.domain.model.Prediction
import com.example.mycycle.domain.model.PredictionMethod
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PredictionEngine {

    companion object {
        private const val FERTILE_BEFORE_OVULATION = 5
        private const val FERTILE_AFTER_OVULATION = 1
        private const val LUTEAL_PHASE_MIN_DAYS = 11
        private const val LUTEAL_PHASE_MAX_DAYS = 17
        private const val MAX_CYCLES_FOR_AVERAGE = 6
        private const val MIN_CONFIDENCE = 0.20f
        private const val MAX_CONFIDENCE = 0.90f
    }

    fun predictFromOnboarding(
        lastPeriodStart: LocalDate,
        cycleLength: Int,
        periodLength: Int,
        stage: CycleStage = CycleStage.ESTABLISHED
    ): Prediction {
        if (stage == CycleStage.PERIODS_STOPPED) {
            return stoppedPrediction(periodLength, stage)
        }

        return calculatePrediction(
            lastPeriodStart = lastPeriodStart,
            avgCycleLength = cycleLength,
            avgPeriodLength = periodLength,
            cycleCount = 0,
            stdDev = 0f,
            spread = 0,
            stage = stage,
            method = PredictionMethod.ONBOARDING_ESTIMATE
        )
    }

    fun predictFromHistory(
        cycles: List<Cycle>,
        fallbackCycleLength: Int,
        fallbackPeriodLength: Int,
        stage: CycleStage = CycleStage.ESTABLISHED
    ): Prediction {
        if (stage == CycleStage.PERIODS_STOPPED) {
            return stoppedPrediction(fallbackPeriodLength, stage)
        }

        val completeCycles = cycles.filter { it.isComplete }

        if (completeCycles.isEmpty()) {
            val lastCycle = cycles.lastOrNull()
            return predictFromOnboarding(
                lastPeriodStart = lastCycle?.startDate ?: LocalDate.now(),
                cycleLength = fallbackCycleLength,
                periodLength = fallbackPeriodLength,
                stage = stage
            )
        }

        val recentCycles = completeCycles.takeLast(MAX_CYCLES_FOR_AVERAGE)
        val lengths = recentCycles.mapNotNull { it.length }
        val periodLengths = recentCycles.map { it.periodLength }

        val avgCycleLength = weightedAverage(lengths)
        val avgPeriodLength = weightedAverage(periodLengths)
        val stdDev = standardDeviation(lengths)
        val spread = if (lengths.isEmpty()) 0 else (lengths.max() - lengths.min())

        return calculatePrediction(
            lastPeriodStart = cycles.last().startDate,
            avgCycleLength = avgCycleLength,
            avgPeriodLength = avgPeriodLength,
            cycleCount = recentCycles.size,
            stdDev = stdDev,
            spread = spread,
            stage = stage,
            method = PredictionMethod.WEIGHTED_AVERAGE
        )
    }

    private fun calculatePrediction(
        lastPeriodStart: LocalDate,
        avgCycleLength: Int,
        avgPeriodLength: Int,
        cycleCount: Int,
        stdDev: Float,
        spread: Int,
        stage: CycleStage,
        method: PredictionMethod
    ): Prediction {
        val centerDate = lastPeriodStart.plusDays(avgCycleLength.toLong())
        val historyRadius = maxOf(
            ceil(spread / 2.0).toInt(),
            ceil(stdDev * 1.5f).toInt()
        )
        val radius = maxOf(minimumWindowRadius(stage), historyRadius)
        val nextPeriodStartWindow = DateRange(
            start = centerDate.minusDays(radius.toLong()),
            end = centerDate.plusDays(radius.toLong())
        )

        val highlyVariable = isHighlyVariable(
            stage = stage,
            averageLength = avgCycleLength,
            spread = spread
        )

        val canEstimateOvulation = stage == CycleStage.ESTABLISHED &&
            cycleCount >= 3 &&
            !highlyVariable

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
                end = it.end.plusDays(FERTILE_AFTER_OVULATION.toLong())
            )
        }

        return Prediction(
            nextPeriodStartWindow = nextPeriodStartWindow,
            expectedPeriodLength = avgPeriodLength.coerceAtLeast(1),
            possiblePregnancyWindow = possiblePregnancyWindow,
            possibleOvulationWindow = possibleOvulationWindow,
            confidence = calculateConfidence(cycleCount, stdDev, avgCycleLength, stage),
            basedOnCycles = cycleCount,
            method = method,
            estimatedCycleLength = avgCycleLength,
            highlyVariable = highlyVariable,
            stage = stage
        )
    }

    private fun stoppedPrediction(periodLength: Int, stage: CycleStage): Prediction = Prediction(
        nextPeriodStartWindow = null,
        expectedPeriodLength = periodLength.coerceAtLeast(1),
        possiblePregnancyWindow = null,
        possibleOvulationWindow = null,
        confidence = 0f,
        basedOnCycles = 0,
        method = PredictionMethod.ONBOARDING_ESTIMATE,
        estimatedCycleLength = null,
        highlyVariable = true,
        stage = stage
    )

    private fun minimumWindowRadius(stage: CycleStage): Int = when (stage) {
        CycleStage.FIRST_YEAR -> 10
        CycleStage.YEARS_ONE_TO_THREE -> 7
        CycleStage.ESTABLISHED -> 3
        CycleStage.CHANGING_WITH_AGE -> 14
        CycleStage.PERIODS_STOPPED -> 0
    }

    private fun isHighlyVariable(
        stage: CycleStage,
        averageLength: Int,
        spread: Int
    ): Boolean = when (stage) {
        CycleStage.FIRST_YEAR -> true
        CycleStage.YEARS_ONE_TO_THREE -> spread > 14 || averageLength !in 21..45
        CycleStage.ESTABLISHED -> spread > 9 || averageLength !in 24..38
        CycleStage.CHANGING_WITH_AGE -> spread >= 7 || averageLength >= 60
        CycleStage.PERIODS_STOPPED -> true
    }

    private fun calculateConfidence(
        cycleCount: Int,
        stdDev: Float,
        avgLength: Int,
        stage: CycleStage
    ): Float {
        if (cycleCount == 0) return MIN_CONFIDENCE

        val dataFactor = (cycleCount.toFloat() / MAX_CYCLES_FOR_AVERAGE).coerceAtMost(1f)
        val regularityFactor = if (avgLength > 0 && stdDev > 0) {
            (1 - stdDev / avgLength).coerceIn(0f, 1f)
        } else {
            1f
        }
        val stageFactor = when (stage) {
            CycleStage.FIRST_YEAR -> 0.45f
            CycleStage.YEARS_ONE_TO_THREE -> 0.65f
            CycleStage.ESTABLISHED -> 1f
            CycleStage.CHANGING_WITH_AGE -> 0.55f
            CycleStage.PERIODS_STOPPED -> 0f
        }

        val rawConfidence = (dataFactor * 0.6f + regularityFactor * 0.4f) * stageFactor

        return (rawConfidence * (MAX_CONFIDENCE - MIN_CONFIDENCE) + MIN_CONFIDENCE)
            .coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE)
    }

    private fun weightedAverage(values: List<Int>): Int {
        if (values.isEmpty()) return 28
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
