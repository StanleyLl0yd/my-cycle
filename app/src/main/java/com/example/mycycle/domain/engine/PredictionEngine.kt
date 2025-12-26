package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.DateRange
import com.example.mycycle.domain.model.Prediction
import com.example.mycycle.domain.model.PredictionMethod
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PredictionEngine {

    companion object {
        private const val LUTEAL_PHASE_DAYS = 14
        private const val FERTILE_BEFORE_OVULATION = 5
        private const val FERTILE_AFTER_OVULATION = 1
        private const val MAX_CYCLES_FOR_AVERAGE = 6
        private const val MIN_CONFIDENCE = 0.25f
        private const val MAX_CONFIDENCE = 0.90f
    }

    fun predictFromOnboarding(
        lastPeriodStart: LocalDate,
        cycleLength: Int,
        periodLength: Int
    ): Prediction {
        return calculatePrediction(
            lastPeriodStart = lastPeriodStart,
            avgCycleLength = cycleLength,
            avgPeriodLength = periodLength,
            cycleCount = 0,
            stdDev = 0f,
            method = PredictionMethod.ONBOARDING_ESTIMATE
        )
    }

    fun predictFromHistory(
        cycles: List<Cycle>,
        fallbackCycleLength: Int,
        fallbackPeriodLength: Int
    ): Prediction {
        val completeCycles = cycles.filter { it.isComplete }

        if (completeCycles.isEmpty()) {
            val lastCycle = cycles.lastOrNull()
            return predictFromOnboarding(
                lastPeriodStart = lastCycle?.startDate ?: LocalDate.now(),
                cycleLength = fallbackCycleLength,
                periodLength = lastCycle?.periodLength ?: fallbackPeriodLength
            )
        }

        val recentCycles = completeCycles.takeLast(MAX_CYCLES_FOR_AVERAGE)
        val lengths = recentCycles.mapNotNull { it.length }
        val periodLengths = recentCycles.map { it.periodLength }

        val avgCycleLength = weightedAverage(lengths)
        val avgPeriodLength = weightedAverage(periodLengths)
        val stdDev = standardDeviation(lengths)

        val lastCycleStart = cycles.last().startDate

        return calculatePrediction(
            lastPeriodStart = lastCycleStart,
            avgCycleLength = avgCycleLength,
            avgPeriodLength = avgPeriodLength,
            cycleCount = recentCycles.size,
            stdDev = stdDev,
            method = PredictionMethod.WEIGHTED_AVERAGE
        )
    }

    private fun calculatePrediction(
        lastPeriodStart: LocalDate,
        avgCycleLength: Int,
        avgPeriodLength: Int,
        cycleCount: Int,
        stdDev: Float,
        method: PredictionMethod
    ): Prediction {
        val nextPeriodStart = lastPeriodStart.plusDays(avgCycleLength.toLong())
        val nextPeriodEnd = nextPeriodStart.plusDays(avgPeriodLength.toLong() - 1)

        val ovulation = nextPeriodStart.minusDays(LUTEAL_PHASE_DAYS.toLong())

        val fertileStart = ovulation.minusDays(FERTILE_BEFORE_OVULATION.toLong())
        val fertileEnd = ovulation.plusDays(FERTILE_AFTER_OVULATION.toLong())

        val confidence = calculateConfidence(cycleCount, stdDev, avgCycleLength)

        return Prediction(
            nextPeriod = DateRange(nextPeriodStart, nextPeriodEnd),
            fertileWindow = DateRange(fertileStart, fertileEnd),
            ovulationDate = ovulation,
            confidence = confidence,
            basedOnCycles = cycleCount,
            method = method
        )
    }

    private fun calculateConfidence(
        cycleCount: Int,
        stdDev: Float,
        avgLength: Int
    ): Float {
        if (cycleCount == 0) return MIN_CONFIDENCE

        val dataFactor = (cycleCount.toFloat() / MAX_CYCLES_FOR_AVERAGE).coerceAtMost(1f)

        val regularityFactor = if (avgLength > 0 && stdDev > 0) {
            (1 - stdDev / avgLength).coerceIn(0f, 1f)
        } else 1f

        val rawConfidence = dataFactor * 0.6f + regularityFactor * 0.4f

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
