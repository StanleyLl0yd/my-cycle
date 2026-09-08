package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.Symptom
import java.time.LocalDate

enum class SymptomTiming {
    PERIOD,
    BEFORE_PERIOD,
    OTHER,
    MIXED
}

data class SymptomInsight(
    val symptom: Symptom,
    val occurrenceDays: Int,
    val cycleCount: Int,
    val timing: SymptomTiming
)

data class CycleInsights(
    val shortestCycleLength: Int?,
    val longestCycleLength: Int?,
    val trackedDayCount: Int,
    val symptomInsights: List<SymptomInsight>
)

class InsightEngine {

    companion object {
        private const val MAX_RECENT_CYCLES = 6
        private const val MAX_SYMPTOM_INSIGHTS = 3
        private const val DAYS_BEFORE_PERIOD = 3
        private const val MIN_PATTERN_OCCURRENCES = 2
    }

    fun analyze(days: List<CycleDay>, cycles: List<Cycle>): CycleInsights {
        val recentCycles = cycles
            .filter { it.isComplete && it.length != null }
            .takeLast(MAX_RECENT_CYCLES)
        val lengths = recentCycles.mapNotNull { it.length }
        val relevantDays = relevantDays(days, recentCycles)

        return CycleInsights(
            shortestCycleLength = lengths.minOrNull(),
            longestCycleLength = lengths.maxOrNull(),
            trackedDayCount = relevantDays.size,
            symptomInsights = Symptom.entries
                .mapNotNull { symptomInsight(it, relevantDays, recentCycles) }
                .sortedWith(
                    compareByDescending<SymptomInsight> { it.cycleCount }
                        .thenByDescending { it.occurrenceDays }
                )
                .take(MAX_SYMPTOM_INSIGHTS)
        )
    }

    private fun relevantDays(
        days: List<CycleDay>,
        recentCycles: List<Cycle>
    ): List<CycleDay> {
        if (recentCycles.isEmpty()) return emptyList()
        val start = recentCycles.first().startDate.minusDays(DAYS_BEFORE_PERIOD.toLong())
        val end = recentCycles.last().endDate ?: recentCycles.last().periodEndDate
        return days.filter { it.date in start..end }
    }

    private fun symptomInsight(
        symptom: Symptom,
        days: List<CycleDay>,
        cycles: List<Cycle>
    ): SymptomInsight? {
        val matchingDates = days
            .asSequence()
            .filter { symptom in it.symptoms }
            .map { it.date }
            .toList()

        if (matchingDates.size < MIN_PATTERN_OCCURRENCES) return null

        val cycleCount = cycles.count { cycle ->
            val end = cycle.endDate ?: cycle.periodEndDate
            matchingDates.any { it in cycle.startDate..end }
        }
        val timings = matchingDates.map { classifyTiming(it, cycles) }

        return SymptomInsight(
            symptom = symptom,
            occurrenceDays = matchingDates.size,
            cycleCount = cycleCount,
            timing = dominantTiming(timings)
        )
    }

    private fun classifyTiming(
        date: LocalDate,
        cycles: List<Cycle>
    ): SymptomTiming {
        if (cycles.any { date in it.startDate..it.periodEndDate }) {
            return SymptomTiming.PERIOD
        }
        if (
            cycles.any {
                date in it.startDate.minusDays(DAYS_BEFORE_PERIOD.toLong())
                    ..it.startDate.minusDays(1)
            }
        ) {
            return SymptomTiming.BEFORE_PERIOD
        }
        return SymptomTiming.OTHER
    }

    private fun dominantTiming(timings: List<SymptomTiming>): SymptomTiming {
        val counts = timings.groupingBy { it }.eachCount()
        val best = counts.maxByOrNull { it.value } ?: return SymptomTiming.MIXED
        return if (best.value * 2 > timings.size) best.key else SymptomTiming.MIXED
    }
}
