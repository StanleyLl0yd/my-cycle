package com.sl.mycycle.ui.calendar

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.FertilityState
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.PeriodState
import com.sl.mycycle.domain.model.Prediction
import java.time.LocalDate

internal fun resolvePeriodState(
    date: LocalDate,
    cycleDay: CycleDay?,
    prediction: Prediction?
): PeriodState {
    if (cycleDay?.flowIntensity == FlowIntensity.SPOTTING) {
        return PeriodState.CONFIRMED_SPOTTING
    }

    if (cycleDay?.isPeriodBleeding == true) {
        return when (cycleDay.flowIntensity) {
            FlowIntensity.LIGHT -> PeriodState.CONFIRMED_LIGHT
            FlowIntensity.MEDIUM -> PeriodState.CONFIRMED_MEDIUM
            FlowIntensity.HEAVY -> PeriodState.CONFIRMED_HEAVY
            FlowIntensity.SPOTTING -> PeriodState.CONFIRMED_SPOTTING
            null -> PeriodState.CONFIRMED_UNSPECIFIED
        }
    }

    if (cycleDay != null) {
        return PeriodState.NONE
    }

    if (prediction?.nextPeriodStartWindow?.contains(date) == true) {
        return PeriodState.PREDICTED
    }

    return PeriodState.NONE
}

internal fun resolveFertilityState(
    date: LocalDate,
    cycleDay: CycleDay?,
    prediction: Prediction?
): FertilityState {
    if (prediction == null || cycleDay?.isPeriodBleeding == true) {
        return FertilityState.NONE
    }

    if (prediction.possibleOvulationWindow?.contains(date) == true) {
        return FertilityState.OVULATION_PREDICTED
    }

    if (prediction.possiblePregnancyWindow?.contains(date) == true) {
        return FertilityState.FERTILE_PREDICTED
    }

    return FertilityState.NONE
}
