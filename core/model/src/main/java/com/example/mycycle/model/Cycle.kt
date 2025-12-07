package com.example.mycycle.model

import java.time.LocalDate

public data class Cycle(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val averageLengthDays: Int,
    val lutealPhaseDays: Int,
    val confidence: Float,
)
