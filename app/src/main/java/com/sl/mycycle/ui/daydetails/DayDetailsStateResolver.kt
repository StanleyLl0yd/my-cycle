package com.sl.mycycle.ui.daydetails

import com.sl.mycycle.domain.model.CycleDay
import java.time.LocalDate

internal fun resolveDayDetailsRefresh(
    current: DayDetailsState,
    existingDay: CycleDay?,
    date: LocalDate,
    today: LocalDate
): DayDetailsState = when {
    current.isSaved -> current
    current.isDirty -> current.copy(
        isFutureDate = date.isAfter(today),
        isLoading = false
    )
    else -> DayDetailsState(
        date = date,
        hasPeriod = existingDay?.isPeriodBleeding ?: false,
        flowIntensity = existingDay?.flowIntensity,
        mood = existingDay?.mood,
        symptoms = existingDay?.symptoms ?: emptySet(),
        notes = existingDay?.notes ?: "",
        isFutureDate = date.isAfter(today),
        isLoading = false
    )
}
