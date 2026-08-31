package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.CycleNotice
import com.sl.mycycle.domain.model.CycleStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CycleNoticeEvaluatorTest {

    private val evaluator = CycleNoticeEvaluator()

    @Test
    fun firstYearsGapOfNinetyDaysGetsAttentionNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.YEARS_ONE_TO_THREE,
            latestCompletedLength = null,
            currentCycleDay = 90,
            currentPeriodLength = null,
            isPeriodToday = false,
            bleedingToday = false
        )

        assertEquals(CycleNotice.THREE_MONTH_GAP, notice)
    }

    @Test
    fun adultUnexplainedGapOfNinetyDaysGetsAttentionNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.ESTABLISHED,
            latestCompletedLength = null,
            currentCycleDay = 90,
            currentPeriodLength = null,
            isPeriodToday = false,
            bleedingToday = false
        )

        assertEquals(CycleNotice.LONG_UNEXPLAINED_GAP, notice)
    }

    @Test
    fun bleedingAfterYearGapHasHighestPriority() {
        val notice = evaluator.evaluate(
            stage = CycleStage.ESTABLISHED,
            latestCompletedLength = null,
            currentCycleDay = 365,
            currentPeriodLength = 1,
            isPeriodToday = false,
            bleedingToday = true
        )

        assertEquals(CycleNotice.BLEEDING_AFTER_YEAR_GAP, notice)
    }

    @Test
    fun firstYearBleedingLongerThanSevenDaysGetsNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.FIRST_YEAR,
            latestCompletedLength = null,
            currentCycleDay = 8,
            currentPeriodLength = 8,
            isPeriodToday = true,
            bleedingToday = true
        )

        assertEquals(CycleNotice.LONG_BLEEDING, notice)
    }

    @Test
    fun establishedThirtySixDayCurrentCycleGetsRangeNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.ESTABLISHED,
            latestCompletedLength = null,
            currentCycleDay = 36,
            currentPeriodLength = null,
            isPeriodToday = false,
            bleedingToday = false
        )

        assertEquals(CycleNotice.OUTSIDE_COMMON_RANGE, notice)
    }

    @Test
    fun establishedOrdinaryCycleNeedsNoNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.ESTABLISHED,
            latestCompletedLength = 28,
            currentCycleDay = 20,
            currentPeriodLength = null,
            isPeriodToday = false,
            bleedingToday = false
        )

        assertNull(notice)
    }

    @Test
    fun stoppedStageWithoutNewBleedingKeepsStoppedNotice() {
        val notice = evaluator.evaluate(
            stage = CycleStage.PERIODS_STOPPED,
            latestCompletedLength = null,
            currentCycleDay = null,
            currentPeriodLength = null,
            isPeriodToday = false,
            bleedingToday = false
        )

        assertEquals(CycleNotice.PERIODS_STOPPED, notice)
    }
}
