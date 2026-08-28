package com.silverlightning.mycycle.ui.daydetails

import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.FlowIntensity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayDetailsStateResolverTest {

    private val date = LocalDate.of(2026, 8, 28)

    @Test
    fun initialRefreshLoadsStoredDay() {
        val stored = CycleDay(
            date = date,
            hasPeriod = true,
            flowIntensity = FlowIntensity.MEDIUM,
            notes = "stored"
        )

        val result = resolveDayDetailsRefresh(
            current = DayDetailsState(),
            existingDay = stored,
            date = date,
            today = date
        )

        assertEquals(date, result.date)
        assertTrue(result.hasPeriod)
        assertEquals(FlowIntensity.MEDIUM, result.flowIntensity)
        assertEquals("stored", result.notes)
        assertFalse(result.isLoading)
        assertFalse(result.isDirty)
    }

    @Test
    fun dirtyRefreshPreservesEditsAndOnlyUpdatesDateStatus() {
        val edited = DayDetailsState(
            date = date,
            hasPeriod = true,
            flowIntensity = FlowIntensity.HEAVY,
            notes = "unsaved edit",
            isDirty = true
        )
        val stored = CycleDay(
            date = date,
            hasPeriod = false,
            notes = "database value"
        )

        val result = resolveDayDetailsRefresh(
            current = edited,
            existingDay = stored,
            date = date,
            today = date.minusDays(1)
        )

        assertTrue(result.hasPeriod)
        assertEquals(FlowIntensity.HEAVY, result.flowIntensity)
        assertEquals("unsaved edit", result.notes)
        assertTrue(result.isDirty)
        assertTrue(result.isFutureDate)
        assertFalse(result.isLoading)
    }

    @Test
    fun savedStateIgnoresLaterRefreshes() {
        val saved = DayDetailsState(
            date = date,
            notes = "saved",
            isSaved = true
        )

        val result = resolveDayDetailsRefresh(
            current = saved,
            existingDay = CycleDay(date = date, notes = "other"),
            date = date,
            today = date.minusDays(1)
        )

        assertEquals(saved, result)
    }
}
