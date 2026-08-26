package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.CycleStage
import com.example.mycycle.domain.model.PredictionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PredictionEngineTest {

    private val engine = PredictionEngine()

    @Test
    fun onboardingUsesDateRangeInsteadOfExactDay() {
        val start = LocalDate.of(2026, 8, 1)

        val prediction = engine.predictFromOnboarding(
            lastPeriodStart = start,
            cycleLength = 28,
            periodLength = 5,
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(LocalDate.of(2026, 8, 26), prediction.nextPeriodStartWindow?.start)
        assertEquals(LocalDate.of(2026, 9, 1), prediction.nextPeriodStartWindow?.end)
        assertEquals(5, prediction.expectedPeriodLength)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
        assertEquals(PredictionMethod.ONBOARDING_ESTIMATE, prediction.method)
    }

    @Test
    fun firstYearUsesWideWindowAndDoesNotGuessOvulation() {
        val prediction = engine.predictFromOnboarding(
            lastPeriodStart = LocalDate.of(2026, 8, 1),
            cycleLength = 28,
            periodLength = 5,
            stage = CycleStage.FIRST_YEAR
        )

        assertEquals(LocalDate.of(2026, 8, 19), prediction.nextPeriodStartWindow?.start)
        assertEquals(LocalDate.of(2026, 9, 8), prediction.nextPeriodStartWindow?.end)
        assertTrue(prediction.highlyVariable)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
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
            fallbackPeriodLength = 5,
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(5, prediction.expectedPeriodLength)
        assertEquals(LocalDate.of(2026, 8, 26), prediction.nextPeriodStartWindow?.start)
        assertEquals(0, prediction.basedOnCycles)
        assertEquals(PredictionMethod.ONBOARDING_ESTIMATE, prediction.method)
    }

    @Test
    fun stableAdultHistoryCanShowBroadPregnancyEstimate() {
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
            fallbackPeriodLength = 5,
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(LocalDate.of(2026, 4, 24), prediction.nextPeriodStartWindow?.start)
        assertEquals(LocalDate.of(2026, 4, 30), prediction.nextPeriodStartWindow?.end)
        assertEquals(6, prediction.expectedPeriodLength)
        assertEquals(3, prediction.basedOnCycles)
        assertFalse(prediction.highlyVariable)
        assertNotNull(prediction.possibleOvulationWindow)
        assertNotNull(prediction.possiblePregnancyWindow)
        assertTrue(prediction.confidence > 0.20f)
    }

    @Test
    fun changingAdultHistoryStopsOvulationGuess() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 24, 5),
            completeCycle(2, LocalDate.of(2026, 1, 25), 38, 5),
            completeCycle(3, LocalDate.of(2026, 3, 4), 27, 5),
            Cycle(
                id = 4,
                startDate = LocalDate.of(2026, 3, 31),
                endDate = null,
                periodEndDate = LocalDate.of(2026, 4, 4),
                length = null,
                periodLength = 5,
                isComplete = false
            )
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            stage = CycleStage.ESTABLISHED
        )

        assertTrue(prediction.highlyVariable)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
    }

    @Test
    fun longTermUnevenStageAlwaysUsesWideWindowAndNoOvulationGuess() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 26, 5),
            completeCycle(2, LocalDate.of(2026, 1, 27), 42, 5),
            completeCycle(3, LocalDate.of(2026, 3, 10), 31, 5),
            Cycle(
                id = 4,
                startDate = LocalDate.of(2026, 4, 10),
                endDate = null,
                periodEndDate = LocalDate.of(2026, 4, 14),
                length = null,
                periodLength = 5,
                isComplete = false
            )
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            stage = CycleStage.LONG_TERM_UNEVEN
        )

        val window = prediction.nextPeriodStartWindow
        assertNotNull(window)
        assertTrue(window!!.lengthDays >= 29)
        assertTrue(prediction.highlyVariable)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
    }

    @Test
    fun changingWithAgeAlwaysUsesWideWindowAndNoOvulationGuess() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 27, 5),
            completeCycle(2, LocalDate.of(2026, 1, 28), 34, 5),
            completeCycle(3, LocalDate.of(2026, 3, 3), 29, 5),
            Cycle(
                id = 4,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = null,
                periodEndDate = LocalDate.of(2026, 4, 5),
                length = null,
                periodLength = 5,
                isComplete = false
            )
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            stage = CycleStage.CHANGING_WITH_AGE
        )

        val window = prediction.nextPeriodStartWindow
        assertNotNull(window)
        assertTrue(window!!.lengthDays >= 29)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
    }

    @Test
    fun periodsStoppedDisablesNextPeriodPrediction() {
        val prediction = engine.predictFromOnboarding(
            lastPeriodStart = LocalDate.of(2025, 1, 1),
            cycleLength = 28,
            periodLength = 5,
            stage = CycleStage.PERIODS_STOPPED
        )

        assertNull(prediction.nextPeriodStartWindow)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
        assertEquals(0f, prediction.confidence)
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
