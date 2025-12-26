package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.CyclePhase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CyclePhaseCalculator {

    fun getPhase(
        cycleDay: Int,
        cycleLength: Int,
        periodLength: Int
    ): CyclePhase {
        if (cycleDay <= periodLength) {
            return CyclePhase.MENSTRUAL
        }

        val ovulationDay = cycleLength - 14

        return when {
            cycleDay in (ovulationDay - 2)..(ovulationDay + 1) -> CyclePhase.OVULATORY
            cycleDay < ovulationDay - 2 -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
    }

    fun getCycleDay(date: LocalDate, lastPeriodStart: LocalDate): Int {
        return ChronoUnit.DAYS.between(lastPeriodStart, date).toInt() + 1
    }

    fun getDaysUntilPeriod(date: LocalDate, nextPeriodStart: LocalDate): Int {
        return ChronoUnit.DAYS.between(date, nextPeriodStart).toInt()
    }

    fun getDaysUntilFertileWindow(date: LocalDate, fertileStart: LocalDate): Int {
        return ChronoUnit.DAYS.between(date, fertileStart).toInt()
    }
}
