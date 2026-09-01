package com.sl.mycycle.domain.model

import java.time.LocalDate

data class Prediction(
    val nextPeriodStartWindow: DateRange?,
    val possiblePregnancyWindow: DateRange?,
    val possibleOvulationWindow: DateRange?,
    val basedOnCycles: Int,
    val highlyVariable: Boolean,
    val outsideCommonRange: Boolean,
    val stage: CycleStage
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
