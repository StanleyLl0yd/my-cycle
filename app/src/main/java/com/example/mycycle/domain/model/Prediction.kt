package com.example.mycycle.domain.model

import java.time.LocalDate

data class Prediction(
    val nextPeriod: DateRange,
    val fertileWindow: DateRange,
    val ovulationDate: LocalDate,
    val confidence: Float,
    val basedOnCycles: Int,
    val method: PredictionMethod
)

data class DateRange(
    val start: LocalDate,
    val end: LocalDate
) {
    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    val lengthDays: Int
        get() = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
}

enum class PredictionMethod {
    ONBOARDING_ESTIMATE,
    CALENDAR_RHYTHM,
    WEIGHTED_AVERAGE
}
