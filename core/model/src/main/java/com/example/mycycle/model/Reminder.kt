package com.example.mycycle.model

import java.time.LocalTime

public data class Reminder(
    val id: String,
    val type: ReminderType,
    val time: LocalTime,
    val enabled: Boolean,
)

public enum class ReminderType { CYCLE_START, CYCLE_END, OVULATION, MEDICATION, CUSTOM }
