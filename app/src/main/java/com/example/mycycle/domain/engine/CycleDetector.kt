package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.CycleDay
import java.time.temporal.ChronoUnit

class CycleDetector {

    companion object {
        private const val MAX_PERIOD_GAP_DAYS = 2
        private const val MIN_CYCLE_LENGTH = 15
    }

    fun detectCycles(periodDays: List<CycleDay>): List<Cycle> {
        val sortedDays = periodDays
            .filter { it.hasPeriod }
            .sortedBy { it.date }

        if (sortedDays.isEmpty()) return emptyList()

        val cycles = mutableListOf<Cycle>()
        var cycleId = 1
        var currentPeriodStart = sortedDays.first().date
        var currentPeriodEnd = currentPeriodStart
        var previousAcceptedPeriodDate = currentPeriodStart

        for (i in 1 until sortedDays.size) {
            val day = sortedDays[i]
            val gapFromAcceptedPeriodDay = ChronoUnit.DAYS.between(
                previousAcceptedPeriodDate,
                day.date
            )
            val daysFromCycleStart = ChronoUnit.DAYS.between(
                currentPeriodStart,
                day.date
            )

            when {
                gapFromAcceptedPeriodDay <= MAX_PERIOD_GAP_DAYS -> {
                    currentPeriodEnd = day.date
                    previousAcceptedPeriodDate = day.date
                }

                daysFromCycleStart >= MIN_CYCLE_LENGTH -> {
                    cycles.add(
                        Cycle(
                            id = cycleId++,
                            startDate = currentPeriodStart,
                            endDate = day.date.minusDays(1),
                            periodEndDate = currentPeriodEnd,
                            length = daysFromCycleStart.toInt(),
                            periodLength = ChronoUnit.DAYS.between(
                                currentPeriodStart,
                                currentPeriodEnd
                            ).toInt() + 1,
                            isComplete = true
                        )
                    )
                    currentPeriodStart = day.date
                    currentPeriodEnd = day.date
                    previousAcceptedPeriodDate = day.date
                }

                else -> {
                    // A separated bleeding/spotting mark inside the minimum cycle
                    // window is kept in the database and calendar, but it must not
                    // stretch the menstrual period used for cycle statistics.
                }
            }
        }

        cycles.add(
            Cycle(
                id = cycleId,
                startDate = currentPeriodStart,
                endDate = null,
                periodEndDate = currentPeriodEnd,
                length = null,
                periodLength = ChronoUnit.DAYS.between(
                    currentPeriodStart,
                    currentPeriodEnd
                ).toInt() + 1,
                isComplete = false
            )
        )

        return cycles
    }
}
