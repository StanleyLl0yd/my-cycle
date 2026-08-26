package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.FlowIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleDetectorTest {

    private val detector = CycleDetector()

    @Test
    fun consecutivePeriodDaysFormSingleCurrentCycle() {
        val days = (0..4).map { offset ->
            CycleDay(
                date = LocalDate.of(2026, 8, 1).plusDays(offset.toLong()),
                hasPeriod = true
            )
        }

        val cycles = detector.detectCycles(days)

        assertEquals(1, cycles.size)
        assertEquals(LocalDate.of(2026, 8, 1), cycles.single().startDate)
        assertEquals(LocalDate.of(2026, 8, 5), cycles.single().periodEndDate)
        assertEquals(5, cycles.single().periodLength)
        assertFalse(cycles.single().isComplete)
    }

    @Test
    fun smallGapInsidePeriodIsTolerated() {
        val days = listOf(
            CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 4), hasPeriod = true)
        )

        val cycle = detector.detectCycles(days).single()

        assertEquals(LocalDate.of(2026, 8, 4), cycle.periodEndDate)
        assertEquals(4, cycle.periodLength)
    }

    @Test
    fun newPeriodCompletesPreviousCycle() {
        val days = listOf(
            CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 29), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 30), hasPeriod = true)
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertTrue(cycles[0].isComplete)
        assertEquals(28, cycles[0].length)
        assertEquals(2, cycles[0].periodLength)
        assertEquals(LocalDate.of(2026, 8, 29), cycles[1].startDate)
        assertFalse(cycles[1].isComplete)
    }

    @Test
    fun spottingDoesNotStretchPeriodOrStartNewCycle() {
        val days = listOf(
            CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = true),
            CycleDay(
                LocalDate.of(2026, 8, 10),
                hasPeriod = true,
                flowIntensity = FlowIntensity.SPOTTING
            ),
            CycleDay(LocalDate.of(2026, 8, 29), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 30), hasPeriod = true)
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(28, cycles[0].length)
        assertEquals(2, cycles[0].periodLength)
        assertEquals(LocalDate.of(2026, 8, 29), cycles[1].startDate)
    }

    @Test
    fun shortNewCycleIsPreservedInsteadOfHidden() {
        val days = listOf(
            CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 13), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 14), hasPeriod = true)
        )

        val cycles = detector.detectCycles(days)

        assertEquals(2, cycles.size)
        assertEquals(12, cycles[0].length)
        assertEquals(LocalDate.of(2026, 8, 13), cycles[1].startDate)
    }

    @Test
    fun nonPeriodDaysAreIgnored() {
        val days = listOf(
            CycleDay(LocalDate.of(2026, 8, 1), hasPeriod = true),
            CycleDay(LocalDate.of(2026, 8, 2), hasPeriod = false),
            CycleDay(LocalDate.of(2026, 8, 3), hasPeriod = true)
        )

        val cycle = detector.detectCycles(days).single()

        assertEquals(LocalDate.of(2026, 8, 3), cycle.periodEndDate)
        assertEquals(3, cycle.periodLength)
    }
}
