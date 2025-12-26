package com.example.mycycle.domain.model

import java.time.LocalDate

data class DayState(
    val date: LocalDate,
    val cycleDay: Int?,
    val phase: CyclePhase?,
    val periodState: PeriodState,
    val fertilityState: FertilityState,
    val symptoms: Set<Symptom>,
    val mood: Mood?,
    val hasNotes: Boolean,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

enum class PeriodState {
    NONE,
    CONFIRMED_SPOTTING,
    CONFIRMED_LIGHT,
    CONFIRMED_MEDIUM,
    CONFIRMED_HEAVY,
    PREDICTED
}

enum class FertilityState {
    NONE,
    FERTILE_PREDICTED,
    OVULATION_PREDICTED
}
