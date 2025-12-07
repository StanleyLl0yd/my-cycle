package com.example.mycycle.model

import java.time.LocalDate

public data class LogEntry(
    val id: String,
    val date: LocalDate,
    val bleedingLevel: BleedingLevel?,
    val symptoms: List<String>,
    val mood: Mood?,
    val temperatureCelsius: Float?,
    val weightKg: Float?,
    val notes: String?,
)

public enum class BleedingLevel { LIGHT, MEDIUM, HEAVY }
public enum class Mood { POSITIVE, NEUTRAL, NEGATIVE }
