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
        var previousDate = currentPeriodStart

        for (i in 1 until sortedDays.size) {
            val day = sortedDays[i]
            val gap = ChronoUnit.DAYS.between(previousDate, day.date)

            when {
                gap <= MAX_PERIOD_GAP_DAYS -> {
                    currentPeriodEnd = day.date
                }

                gap >= MIN_CYCLE_LENGTH -> {
                    cycles.add(
                        Cycle(
                            id = cycleId++,
                            startDate = currentPeriodStart,
                            endDate = day.date.minusDays(1),
                            periodEndDate = currentPeriodEnd,
                            length = ChronoUnit.DAYS.between(currentPeriodStart, day.date).toInt(),
                            periodLength = ChronoUnit.DAYS.between(
                                currentPeriodStart,
                                currentPeriodEnd
                            ).toInt() + 1,
                            isComplete = true
                        )
                    )
                    currentPeriodStart = day.date
                    currentPeriodEnd = day.date
                }

                else -> {
                    currentPeriodEnd = day.date
                }
            }
            previousDate = day.date
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
