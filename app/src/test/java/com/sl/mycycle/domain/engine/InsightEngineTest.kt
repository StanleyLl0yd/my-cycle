package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.Symptom
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineTest {

    private val engine = InsightEngine()

    @Test
    fun reportsRecentCycleRangeAndRecurringSymptoms() {
        val cycles = listOf(
            cycle(1, LocalDate.of(2026, 1, 1), 28, 5),
            cycle(2, LocalDate.of(2026, 1, 29), 31, 5),
            cycle(3, LocalDate.of(2026, 3, 1), 29, 5)
        )
        val days = listOf(
            day(LocalDate.of(2026, 1, 2), Symptom.CRAMPS),
            day(LocalDate.of(2026, 1, 30), Symptom.CRAMPS),
            day(LocalDate.of(2026, 3, 2), Symptom.CRAMPS),
            day(LocalDate.of(2026, 2, 27), Symptom.HEADACHE),
            day(LocalDate.of(2026, 2, 28), Symptom.HEADACHE)
        )

        val result = engine.analyze(days, cycles)

        assertEquals(28, result.shortestCycleLength)
        assertEquals(31, result.longestCycleLength)
        assertEquals(Symptom.CRAMPS, result.symptomInsights.first().symptom)
        assertEquals(3, result.symptomInsights.first().cycleCount)
        assertEquals(SymptomTiming.PERIOD, result.symptomInsights.first().timing)
        assertTrue(result.symptomInsights.any {
            it.symptom == Symptom.HEADACHE && it.timing == SymptomTiming.BEFORE_PERIOD
        })
    }

    @Test
    fun ignoresSingleSymptomOccurrence() {
        val cycles = listOf(cycle(1, LocalDate.of(2026, 1, 1), 28, 5))
        val days = listOf(day(LocalDate.of(2026, 1, 2), Symptom.NAUSEA))

        val result = engine.analyze(days, cycles)

        assertTrue(result.symptomInsights.isEmpty())
    }

    private fun day(date: LocalDate, symptom: Symptom): CycleDay =
        CycleDay(date = date, symptoms = setOf(symptom))

    private fun cycle(
        id: Int,
        start: LocalDate,
        length: Int,
        periodLength: Int
    ): Cycle = Cycle(
        id = id,
        startDate = start,
        endDate = start.plusDays(length.toLong() - 1),
        periodEndDate = start.plusDays(periodLength.toLong() - 1),
        length = length,
        periodLength = periodLength,
        isComplete = true
    )
}
