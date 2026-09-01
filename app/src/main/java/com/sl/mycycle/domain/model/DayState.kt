package com.sl.mycycle.domain.model

data class DayState(
    val periodState: PeriodState,
    val fertilityState: FertilityState,
    val isToday: Boolean
)

enum class PeriodState {
    NONE,
    CONFIRMED_UNSPECIFIED,
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
