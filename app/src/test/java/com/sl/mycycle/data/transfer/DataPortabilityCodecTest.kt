package com.sl.mycycle.data.transfer

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.Mood
import com.sl.mycycle.domain.model.Symptom
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.domain.model.UserPreferences
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DataPortabilityCodecTest {

    @Test
    fun csvRoundTripPreservesQuotedContent() {
        val day = CycleDay(
            date = LocalDate.of(2026, 8, 31),
            hasPeriod = true,
            flowIntensity = FlowIntensity.MEDIUM,
            mood = Mood.GOOD,
            symptoms = linkedSetOf(Symptom.CRAMPS, Symptom.FATIGUE),
            notes = "Line one, with comma\nLine two \"quoted\""
        )

        assertEquals(listOf(day), CsvCodec.decodeDays(CsvCodec.encodeDays(listOf(day))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun csvRejectsDuplicateDates() {
        CsvCodec.decodeDays(
            "date,period,flow,mood,symptoms,notes\n" +
                "2026-08-01,true,LIGHT,,,first\n" +
                "2026-08-01,true,MEDIUM,,,second\n"
        )
    }

    @Test
    fun backupRoundTripPreservesDiaryAndSettings() {
        val preferences = UserPreferences(
            onboardingCompleted = true,
            initialPeriodDate = LocalDate.of(2026, 8, 1),
            estimatedCycleLength = 30,
            estimatedPeriodLength = 6,
            cycleStage = CycleStage.ESTABLISHED,
            themeMode = ThemeMode.DARK,
            useDynamicColors = false,
            dailyReminderEnabled = true,
            reminderHour = 21,
            reminderMinute = 15,
            appLockEnabled = true,
            protectScreenEnabled = true
        )
        val days = listOf(
            CycleDay(
                date = LocalDate.of(2026, 8, 1),
                hasPeriod = true,
                flowIntensity = FlowIntensity.LIGHT,
                symptoms = setOf(Symptom.HEADACHE),
                notes = "Saved locally"
            )
        )

        val restored = BackupCodec.decode(BackupCodec.encode(preferences, days))

        assertEquals(preferences, restored.preferences)
        assertEquals(days, restored.days)
    }

    @Test(expected = IllegalArgumentException::class)
    fun backupRejectsFutureInitialPeriodDate() {
        val preferences = UserPreferences(
            initialPeriodDate = LocalDate.now().plusDays(1)
        )

        BackupCodec.decode(BackupCodec.encode(preferences, emptyList()))
    }
}
