package com.silverlightning.mycycle.ui.calendar

import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.FlowIntensity
import java.time.LocalDate

internal fun historicalPeriodDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    require(!endDate.isBefore(startDate))

    return generateSequence(startDate) { current ->
        current.plusDays(1).takeUnless { it.isAfter(endDate) }
    }.toList()
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
