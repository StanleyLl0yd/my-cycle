package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.PredictionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PredictionEngineTest {

    private val engine = PredictionEngine()

    @Test
    fun onboardingPredictionUsesConfiguredLengths() {
        val start = LocalDate.of(2026, 8, 1)

        val prediction = engine.predictFromOnboarding(
            lastPeriodStart = start,
            cycleLength = 28,
            periodLength = 5
        )

        assertEquals(LocalDate.of(2026, 8, 29), prediction.nextPeriod.start)
        assertEquals(LocalDate.of(2026, 9, 2), prediction.nextPeriod.end)
        assertEquals(5, prediction.nextPeriod.lengthDays)
        assertEquals(LocalDate.of(2026, 8, 15), prediction.ovulationDate)
        assertEquals(PredictionMethod.ONBOARDING_ESTIMATE, prediction.method)
    }

    @Test
    fun incompleteFirstCycleDoesNotCollapsePeriodToOneDay() {
        val start = LocalDate.of(2026, 8, 1)
        val currentCycle = Cycle(
            id = 1,
            startDate = start,
            endDate = null,
            periodEndDate = start,
            length = null,
            periodLength = 1,
            isComplete = false
        )

        val prediction = engine.predictFromHistory(
            cycles = listOf(currentCycle),
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5
        )

        assertEquals(5, prediction.nextPeriod.lengthDays)
        assertEquals(LocalDate.of(2026, 8, 29), prediction.nextPeriod.start)
        assertEquals(0, prediction.basedOnCycles)
        assertEquals(PredictionMethod.ONBOARDING_ESTIMATE, prediction.method)
    }

    @Test
    fun recentCyclesAreWeightedTowardNewestData() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 28, 5),
            completeCycle(2, LocalDate.of(2026, 1, 29), 29, 5),
            completeCycle(3, LocalDate.of(2026, 2, 27), 30, 6),
            Cycle(
                id = 4,
                startDate = LocalDate.of(2026, 3, 29),
                endDate = null,
                periodEndDate = LocalDate.of(2026, 4, 2),
                length = null,
                periodLength = 5,
                isComplete = false
            )
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5
        )

        // Weighted average: (28*1 + 29*2 + 30*3) / 6 = 29.33 -> 29.
        assertEquals(LocalDate.of(2026, 4, 27), prediction.nextPeriod.start)
        assertEquals(5, prediction.nextPeriod.lengthDays)
        assertEquals(3, prediction.basedOnCycles)
        assertEquals(PredictionMethod.WEIGHTED_AVERAGE, prediction.method)
        assertTrue(prediction.confidence > 0.25f)
    }

    private fun completeCycle(
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
