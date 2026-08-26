package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.FlowIntensity
import java.time.temporal.ChronoUnit

class CycleDetector {

    companion object {
        private const val MAX_PERIOD_GAP_DAYS = 2
    }

    fun detectCycles(days: List<CycleDay>): List<Cycle> {
        val sortedDays = days
            .filter { it.hasPeriod && it.flowIntensity != FlowIntensity.SPOTTING }
            .sortedBy { it.date }

        if (sortedDays.isEmpty()) return emptyList()

        val cycles = mutableListOf<Cycle>()
        var cycleId = 1
        var currentPeriodStart = sortedDays.first().date
        var currentPeriodEnd = currentPeriodStart
        var previousPeriodDate = currentPeriodStart

        for (i in 1 until sortedDays.size) {
            val day = sortedDays[i]
            val gapFromPreviousPeriodDay = ChronoUnit.DAYS.between(
                previousPeriodDate,
                day.date
            )

            if (gapFromPreviousPeriodDay <= MAX_PERIOD_GAP_DAYS) {
                currentPeriodEnd = day.date
                previousPeriodDate = day.date
                continue
            }

            val cycleLength = ChronoUnit.DAYS.between(
                currentPeriodStart,
                day.date
            ).toInt()

            cycles.add(
                Cycle(
                    id = cycleId++,
                    startDate = currentPeriodStart,
                    endDate = day.date.minusDays(1),
                    periodEndDate = currentPeriodEnd,
                    length = cycleLength,
                    periodLength = ChronoUnit.DAYS.between(
                        currentPeriodStart,
                        currentPeriodEnd
                    ).toInt() + 1,
                    isComplete = true
                )
            )

            currentPeriodStart = day.date
            currentPeriodEnd = day.date
            previousPeriodDate = day.date
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
