package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.PredictionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PredictionEngineTest {

    private val engine = PredictionEngine()
    private val referenceDate = LocalDate.of(2026, 4, 10)

    @Test
    fun onboardingUsesDateRangeInsteadOfExactDay() {
        val prediction = engine.predictFromOnboarding(
            lastPeriodStart = LocalDate.of(2026, 8, 1),
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
    fun unsetStageStaysWideEvenWithStableHistory() {
        val prediction = engine.predictFromHistory(
            cycles = stableHistory(),
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = referenceDate,
            stage = CycleStage.NOT_SET
        )

        val window = prediction.nextPeriodStartWindow
        assertNotNull(window)
        assertTrue(window!!.lengthDays >= 29)
        assertTrue(prediction.highlyVariable)
        assertEquals(CycleStage.NOT_SET, prediction.stage)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
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
    fun incompleteOnboardingCycleUsesFallbackPeriodLength() {
        val start = LocalDate.of(2026, 8, 1)
        val prediction = engine.predictFromHistory(
            cycles = listOf(
                Cycle(
                    id = 1,
                    startDate = start,
                    endDate = null,
                    periodEndDate = start,
                    length = null,
                    periodLength = null,
                    isComplete = false
                )
            ),
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 8, 10),
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(5, prediction.expectedPeriodLength)
        assertEquals(LocalDate.of(2026, 8, 26), prediction.nextPeriodStartWindow?.start)
        assertEquals(0, prediction.basedOnCycles)
        assertEquals(PredictionMethod.ONBOARDING_ESTIMATE, prediction.method)
    }

    @Test
    fun oneKnownCompletedPeriodLengthDoesNotReplaceFallback() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 28, null),
            completeCycle(2, LocalDate.of(2026, 1, 29), 28, 5),
            currentCycle(3, LocalDate.of(2026, 2, 26), 5)
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 6,
            referenceDate = referenceDate,
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(6, prediction.expectedPeriodLength)
    }

    @Test
    fun stableAdultHistoryCanShowBroadPregnancyEstimate() {
        val prediction = engine.predictFromHistory(
            cycles = stableHistory(),
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = referenceDate,
            stage = CycleStage.ESTABLISHED
        )

        assertEquals(LocalDate.of(2026, 4, 24), prediction.nextPeriodStartWindow?.start)
        assertEquals(LocalDate.of(2026, 4, 30), prediction.nextPeriodStartWindow?.end)
        assertEquals(6, prediction.expectedPeriodLength)
        assertEquals(3, prediction.basedOnCycles)
        assertFalse(prediction.highlyVariable)
        assertFalse(prediction.outsideCommonRange)
        assertNotNull(prediction.possibleOvulationWindow)
        assertNotNull(prediction.possiblePregnancyWindow)
        assertEquals(
            prediction.possibleOvulationWindow?.start?.minusDays(5),
            prediction.possiblePregnancyWindow?.start
        )
        assertEquals(
            prediction.possibleOvulationWindow?.end,
            prediction.possiblePregnancyWindow?.end
        )
    }

    @Test
    fun steadyThirtySixDayCyclesAreNotCalledHighlyVariable() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 36, 5),
            completeCycle(2, LocalDate.of(2026, 2, 6), 36, 5),
            completeCycle(3, LocalDate.of(2026, 3, 14), 36, 5),
            currentCycle(4, LocalDate.of(2026, 4, 19), 5)
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = LocalDate.of(2026, 4, 20),
            stage = CycleStage.ESTABLISHED
        )

        assertFalse(prediction.highlyVariable)
        assertTrue(prediction.outsideCommonRange)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
    }

    @Test
    fun changingAdultHistoryStopsOvulationGuess() {
        val cycles = listOf(
            completeCycle(1, LocalDate.of(2026, 1, 1), 24, 5),
            completeCycle(2, LocalDate.of(2026, 1, 25), 38, 5),
            completeCycle(3, LocalDate.of(2026, 3, 4), 27, 5),
            currentCycle(4, LocalDate.of(2026, 3, 31), 5)
        )

        val prediction = engine.predictFromHistory(
            cycles = cycles,
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = referenceDate,
            stage = CycleStage.ESTABLISHED
        )

        assertTrue(prediction.highlyVariable)
        assertNull(prediction.possibleOvulationWindow)
        assertNull(prediction.possiblePregnancyWindow)
    }

    @Test
    fun longTermUnevenStageAlwaysUsesWideWindowAndNoOvulationGuess() {
        val prediction = engine.predictFromHistory(
            cycles = stableHistory(),
            fallbackCycleLength = 28,
            fallbackPeriodLength = 5,
            referenceDate = referenceDate,
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

    private fun stableHistory(): List<Cycle> = listOf(
        completeCycle(1, LocalDate.of(2026, 1, 1), 28, 5),
        completeCycle(2, LocalDate.of(2026, 1, 29), 29, 5),
        completeCycle(3, LocalDate.of(2026, 2, 27), 30, 6),
        currentCycle(4, LocalDate.of(2026, 3, 29), 5)
    )

    private fun completeCycle(
        id: Int,
        start: LocalDate,
        length: Int,
        periodLength: Int?
    ): Cycle = Cycle(
        id = id,
        startDate = start,
        endDate = start.plusDays(length.toLong() - 1),
        periodEndDate = start.plusDays(((periodLength ?: 1) - 1).toLong()),
        length = length,
        periodLength = periodLength,
        isComplete = true
    )

    private fun currentCycle(
        id: Int,
        start: LocalDate,
        periodLength: Int?
    ): Cycle = Cycle(
        id = id,
        startDate = start,
        endDate = null,
        periodEndDate = start.plusDays(((periodLength ?: 1) - 1).toLong()),
        length = null,
        periodLength = periodLength,
        isComplete = false
    )
}
