package com.sl.mycycle.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendResultTest {

    @Test
    fun successfulBlockReturnsValue() = runBlocking {
        val result = runSuspendCatching { 42 }

        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun regularFailureIsReturned() = runBlocking {
        val result = runSuspendCatching<Int> { error("failed") }

        assertTrue(result.isFailure)
    }

    @Test
    fun cancellationIsRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                runSuspendCatching<Unit> { throw CancellationException("cancelled") }
            }
        }
    }
}
