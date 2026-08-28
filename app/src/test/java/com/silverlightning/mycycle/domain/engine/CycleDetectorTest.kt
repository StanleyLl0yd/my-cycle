package com.silverlightning.mycycle.domain.engine

import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.FlowIntensity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleDetectorTest {

    private val detector = CycleDetector()

    @Test
    fun consecutivePeriodDaysFormSingleCurrentCycle() {
        val days = (0..4).map { offset ->
            period(LocalDate.of(2026, 8, 1).plusDays(offset.toLong()))
        }

        val cycle = detector.detectCycles(days).single()

        assertEquals(LocalDate.of(2026, 8, 1), cycle.startDate)
        assertEquals(LocalDate.of(2026, 8, 5), cycle.periodEndDate)
        assertEquals(5, cycle.periodLength)
        assertFalse(cycle.isComplete)
    }

    @Test
    fun unrecordedDayInsidePeriodCanBeBridged() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            period(LocalDate.of(2026, 8, 3))
        )

        val cycle = detector.detectCycles(days).single()

        assertEquals(LocalDate.of(2026, 8, 3), cycle.periodEndDate)
        assertEquals(3, cycle.periodLength)
    }

    @Test
    fun explicitNoBloodDayBreaksPeriod() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = false),
            period(LocalDate.of(2026, 8, 3))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(LocalDate.of(2026, 8, 1), cycles[0].periodEndDate)
        assertEquals(1, cycles[0].periodLength)
        assertEquals(LocalDate.of(2026, 8, 3), cycles[1].startDate)
    }

    @Test
    fun spottingBreaksPeriodButDoesNotBecomePeriod() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            CycleDay(
                date = LocalDate.of(2026, 8, 2),
                hasPeriod = false,
                flowIntensity = FlowIntensity.SPOTTING
            ),
            period(LocalDate.of(2026, 8, 3))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(LocalDate.of(2026, 8, 1), cycles[0].periodEndDate)
        assertEquals(1, cycles[0].periodLength)
        assertEquals(LocalDate.of(2026, 8, 3), cycles[1].startDate)
    }

    @Test
    fun legacySpottingMarkedAsPeriodDoesNotStartCycle() {
        val days = listOf(
            CycleDay(
                date = LocalDate.of(2026, 8, 10),
                hasPeriod = true,
                flowIntensity = FlowIntensity.SPOTTING
            )
        )

        assertTrue(detector.detectCycles(days).isEmpty())
    }

    @Test
    fun onboardingStartWithoutFlowKeepsPeriodLengthUnknown() {
        val cycle = detector.detectCycles(
            listOf(CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true))
        ).single()

        assertNull(cycle.periodLength)
    }

    @Test
    fun onboardingStartStaysUnknownAfterCycleCompletes() {
        val cycles = detector.detectCycles(
            listOf(
                CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
                period(LocalDate.of(2026, 8, 29))
            )
        )

        assertEquals(2, cycles.size)
        assertTrue(cycles[0].isComplete)
        assertEquals(28, cycles[0].length)
        assertNull(cycles[0].periodLength)
    }

    @Test
    fun newPeriodDoesNotInventPreviousPeriodEnd() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            period(LocalDate.of(2026, 8, 2)),
            period(LocalDate.of(2026, 8, 29)),
            period(LocalDate.of(2026, 8, 30))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertTrue(cycles[0].isComplete)
        assertEquals(28, cycles[0].length)
        assertNull(cycles[0].periodLength)
        assertEquals(LocalDate.of(2026, 8, 29), cycles[1].startDate)
        assertFalse(cycles[1].isComplete)
    }

    @Test
    fun explicitNoBloodAfterPeriodConfirmsCompletedLength() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            period(LocalDate.of(2026, 8, 2)),
            CycleDay(LocalDate.of(2026, 8, 3), hasPeriod = false),
            period(LocalDate.of(2026, 8, 29))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(2, cycles[0].periodLength)
    }

    @Test
    fun bridgedMissingDayKeepsCompletedLengthUnknown() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            period(LocalDate.of(2026, 8, 3)),
            CycleDay(LocalDate.of(2026, 8, 4), hasPeriod = false),
            period(LocalDate.of(2026, 8, 29))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertNull(cycles[0].periodLength)
    }

    @Test
    fun shortNewCycleIsPreservedInsteadOfHidden() {
        val days = listOf(
            period(LocalDate.of(2026, 8, 1)),
            period(LocalDate.of(2026, 8, 2)),
            period(LocalDate.of(2026, 8, 13)),
            period(LocalDate.of(2026, 8, 14))
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(12, cycles[0].length)
        assertEquals(LocalDate.of(2026, 8, 13), cycles[1].startDate)
    }

    private fun period(date: LocalDate) = CycleDay(
        date = date,
        hasPeriod = true,
        flowIntensity = FlowIntensity.MEDIUM
    )
}
