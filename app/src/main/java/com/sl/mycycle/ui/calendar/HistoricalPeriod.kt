package com.sl.mycycle.ui.calendar

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.FlowIntensity
import java.time.LocalDate

internal fun historicalPeriodDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    require(!endDate.isBefore(startDate))

    return generateSequence(startDate) { current ->
        current.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.toList()
}

internal fun historicalPeriodBoundaryDate(endDate: LocalDate, today: LocalDate): LocalDate? {
    return endDate.plusDays(1).takeUnless { it.isAfter(today) }
}

internal fun mergeHistoricalPeriodDay(date: LocalDate, existing: CycleDay?): CycleDay {
    return existing?.copy(
        date = date,
        hasPeriod = true,
        flowIntensity = existing.flowIntensity.takeUnless { it == FlowIntensity.SPOTTING }
    ) ?: CycleDay(
        date = date,
        hasPeriod = true
    )
}
