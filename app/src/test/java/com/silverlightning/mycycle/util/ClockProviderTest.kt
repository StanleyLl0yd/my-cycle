package com.silverlightning.mycycle.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockProviderTest {

    @Test
    fun todayUsesCurrentClockFromFactory() {
        val instant = Instant.parse("2026-08-28T10:00:00Z")
        var currentClock: Clock = Clock.fixed(instant, ZoneOffset.UTC)
        val provider = ClockProvider { currentClock }

        assertEquals(LocalDate.of(2026, 8, 28), provider.today())

        currentClock = Clock.fixed(instant, ZoneOffset.ofHours(14))
        assertEquals(LocalDate.of(2026, 8, 29), provider.today())

        currentClock = Clock.fixed(instant, ZoneOffset.ofHours(-12))
        assertEquals(LocalDate.of(2026, 8, 27), provider.today())
    }
}
