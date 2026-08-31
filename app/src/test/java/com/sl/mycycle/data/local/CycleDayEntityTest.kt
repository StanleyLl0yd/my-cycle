package com.sl.mycycle.data.local

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.FlowIntensity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleDayEntityTest {

    @Test
    fun legacySpottingMarkedAsPeriodIsNormalized() {
        val domain = CycleDayEntity(
            date = LocalDate.of(2026, 8, 10),
            hasPeriod = true,
            flowIntensityLevel = FlowIntensity.SPOTTING.level
        ).toDomain()

        assertFalse(domain.hasPeriod)
        assertFalse(domain.isPeriodBleeding)
    }

    @Test
    fun realFlowIsNormalizedAsPeriodBleeding() {
        val domain = CycleDayEntity(
            date = LocalDate.of(2026, 8, 10),
            hasPeriod = false,
            flowIntensityLevel = FlowIntensity.HEAVY.level
        ).toDomain()

        assertTrue(domain.hasPeriod)
        assertTrue(domain.isPeriodBleeding)
    }

    @Test
    fun savingSpottingNeverPersistsPeriodFlag() {
        val entity = CycleDayEntity.fromDomain(
            CycleDay(
                date = LocalDate.of(2026, 8, 10),
                hasPeriod = true,
                flowIntensity = FlowIntensity.SPOTTING
            )
        )

        assertFalse(entity.hasPeriod)
    }
}
