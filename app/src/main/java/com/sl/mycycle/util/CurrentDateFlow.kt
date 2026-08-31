package com.sl.mycycle.util

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MAX_DATE_RECHECK_MILLIS = 60_000L

class ClockProvider(
    private val clockFactory: () -> Clock = { Clock.systemDefaultZone() }
) {
    fun clock(): Clock = clockFactory()

    fun today(): LocalDate = LocalDate.now(clock())
}

fun currentDateFlow(clockProvider: ClockProvider): Flow<LocalDate> = flow {
    var lastEmittedDate: LocalDate? = null

    while (true) {
        val clock = clockProvider.clock()
        val today = LocalDate.now(clock)
        if (today != lastEmittedDate) {
            emit(today)
            lastEmittedDate = today
        }

        val nextMidnight = today.plusDays(1)
            .atStartOfDay(clock.zone)
            .toInstant()
        val untilMidnight = Duration.between(clock.instant(), nextMidnight)
            .toMillis()
            .coerceAtLeast(1_000L)

        delay(minOf(untilMidnight, MAX_DATE_RECHECK_MILLIS))
    }
}
