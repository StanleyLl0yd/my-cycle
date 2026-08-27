package com.silverlightning.mycycle.domain.engine

import com.silverlightning.mycycle.domain.model.Cycle
import com.silverlightning.mycycle.domain.model.CycleDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CycleDetector {

    companion object {
        private const val MAX_PERIOD_GAP_DAYS = 2
    }

    fun detectCycles(days: List<CycleDay>): List<Cycle> {
        val daysByDate = days.associateBy { it.date }
        val periodDays = days
            .filter { it.isPeriodBleeding }
            .sortedBy { it.date }

        if (periodDays.isEmpty()) return emptyList()

        val cycles = mutableListOf<Cycle>()
        var cycleId = 1
        var currentPeriodStart = periodDays.first().date
        var currentPeriodEnd = currentPeriodStart
        var currentPeriodLengthKnown = periodDays.first().flowIntensity != null
        var previousPeriodDate = currentPeriodStart

        for (index in 1 until periodDays.size) {
            val day = periodDays[index]
            val gap = ChronoUnit.DAYS.between(previousPeriodDate, day.date)
            val canBridgeMissingDay =
                gap <= MAX_PERIOD_GAP_DAYS &&
                    !hasExplicitBreak(daysByDate, previousPeriodDate, day.date)

            if (canBridgeMissingDay) {
                currentPeriodEnd = day.date
                currentPeriodLengthKnown =
                    currentPeriodLengthKnown || day.flowIntensity != null
                previousPeriodDate = day.date
                continue
            }

            cycles += Cycle(
                id = cycleId++,
                startDate = currentPeriodStart,
                endDate = day.date.minusDays(1),
                periodEndDate = currentPeriodEnd,
                length = ChronoUnit.DAYS.between(currentPeriodStart, day.date).toInt(),
                periodLength = periodLength(
                    start = currentPeriodStart,
                    end = currentPeriodEnd,
                    isKnown = currentPeriodLengthKnown
                ),
                isComplete = true
            )

            currentPeriodStart = day.date
            currentPeriodEnd = day.date
            currentPeriodLengthKnown = day.flowIntensity != null
            previousPeriodDate = day.date
        }

        cycles += Cycle(
            id = cycleId,
            startDate = currentPeriodStart,
            endDate = null,
            periodEndDate = currentPeriodEnd,
            length = null,
            periodLength = periodLength(
                start = currentPeriodStart,
                end = currentPeriodEnd,
                isKnown = currentPeriodLengthKnown
            ),
            isComplete = false
        )

        return cycles
    }

    private fun hasExplicitBreak(
        daysByDate: Map<LocalDate, CycleDay>,
        previousPeriodDate: LocalDate,
        nextPeriodDate: LocalDate
    ): Boolean {
        var date = previousPeriodDate.plusDays(1)
        while (date.isBefore(nextPeriodDate)) {
            val entry = daysByDate[date]
            if (entry != null && !entry.isPeriodBleeding) {
                return true
            }
            date = date.plusDays(1)
        }
        return false
    }

    private fun periodLength(
        start: LocalDate,
        end: LocalDate,
        isKnown: Boolean
    ): Int? = if (isKnown) {
        ChronoUnit.DAYS.between(start, end).toInt() + 1
    } else {
        null
    }
}
