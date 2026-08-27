package com.example.mycycle.util

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun currentDateFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1)
            .atStartOfDay(clock.zone)
            .toInstant()
        val delayMillis = Duration.between(clock.instant(), nextMidnight)
            .toMillis()
            .coerceAtLeast(1_000L)
        delay(delayMillis)
    }
}
