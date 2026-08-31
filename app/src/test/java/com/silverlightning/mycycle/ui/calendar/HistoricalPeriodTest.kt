package com.silverlightning.mycycle.ui.calendar

import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.FlowIntensity
import com.silverlightning.mycycle.domain.model.Mood
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalPeriodTest {

    @Test
    fun `date range includes both ends`() {
        val start = LocalDate.of(2026, 6, 3)
        val end = LocalDate.of(2026, 6, 6)

        val dates = historicalPeriodDates(start, end)

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 4),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 6)
            ),
            dates
        )
    }

    @Test
    fun `boundary is recorded only when next day is not in the future`() {
        val end = LocalDate.of(2026, 6, 6)

        assertEquals(
            LocalDate.of(2026, 6, 7),
            historicalPeriodBoundaryDate(end, LocalDate.of(2026, 6, 7))
        )
        assertNull(historicalPeriodBoundaryDate(end, end))
    }

    @Test
    fun `existing details are preserved when period is added`() {
        val date = LocalDate.of(2026, 6, 3)
        val existing = CycleDay(
            date = date,
            hasPeriod = false,
            mood = Mood.GOOD,
            notes = "note"
        )

        val merged = mergeHistoricalPeriodDay(date, existing)

        assertTrue(merged.hasPeriod)
        assertEquals(Mood.GOOD, merged.mood)
        assertEquals("note", merged.notes)
    }

    @Test
    fun `spotting is replaced by confirmed period with unknown intensity`() {
        val date = LocalDate.of(2026, 6, 3)
        val existing = CycleDay(
            date = date,
            flowIntensity = FlowIntensity.SPOTTING
        )

        val merged = mergeHistoricalPeriodDay(date, existing)

        assertTrue(merged.hasPeriod)
        assertNull(merged.flowIntensity)
    }
}
