package com.sl.mycycle.domain.engine

import com.sl.mycycle.domain.model.CycleNotice
import com.sl.mycycle.domain.model.CycleStage

class CycleNoticeEvaluator {

    fun evaluate(
        stage: CycleStage,
        latestCompletedLength: Int?,
        currentCycleDay: Int?,
        currentPeriodLength: Int?,
        isPeriodToday: Boolean,
        bleedingToday: Boolean
    ): CycleNotice? {
        val gapDays = maxOf(latestCompletedLength ?: 0, currentCycleDay ?: 0)

        return when {
            hasBleedingAfterYearGap(
                stage = stage,
                latestCompletedLength = latestCompletedLength,
                currentCycleDay = currentCycleDay,
                bleedingToday = bleedingToday
            ) -> CycleNotice.BLEEDING_AFTER_YEAR_GAP
            hasLongBleeding(
                stage = stage,
                currentPeriodLength = currentPeriodLength,
                isPeriodToday = isPeriodToday
            ) -> CycleNotice.LONG_BLEEDING
            isEarlyStage(stage) && gapDays >= LONG_GAP_DAYS -> CycleNotice.THREE_MONTH_GAP
            isEstablishedStage(stage) && gapDays >= LONG_GAP_DAYS -> CycleNotice.LONG_UNEXPLAINED_GAP
            isOutsideCommonRange(stage, latestCompletedLength, currentCycleDay) ->
                CycleNotice.OUTSIDE_COMMON_RANGE
            else -> noticeForStage(stage)
        }
    }

    private fun hasBleedingAfterYearGap(
        stage: CycleStage,
        latestCompletedLength: Int?,
        currentCycleDay: Int?,
        bleedingToday: Boolean
    ): Boolean = bleedingToday && (
        stage == CycleStage.PERIODS_STOPPED ||
            latestCompletedLength?.let { it >= YEAR_GAP_DAYS } == true ||
            (currentCycleDay ?: 0) >= YEAR_GAP_DAYS
        )

    private fun hasLongBleeding(
        stage: CycleStage,
        currentPeriodLength: Int?,
        isPeriodToday: Boolean
    ): Boolean {
        val limit = when (stage) {
            CycleStage.FIRST_YEAR,
            CycleStage.YEARS_ONE_TO_THREE -> EARLY_STAGE_BLEEDING_LIMIT_DAYS
            else -> DEFAULT_BLEEDING_LIMIT_DAYS
        }
        return isPeriodToday && currentPeriodLength != null && currentPeriodLength > limit
    }

    private fun isEarlyStage(stage: CycleStage): Boolean =
        stage == CycleStage.FIRST_YEAR || stage == CycleStage.YEARS_ONE_TO_THREE

    private fun isEstablishedStage(stage: CycleStage): Boolean =
        stage == CycleStage.ESTABLISHED || stage == CycleStage.LONG_TERM_UNEVEN

    private fun isOutsideCommonRange(
        stage: CycleStage,
        latestCompletedLength: Int?,
        currentCycleDay: Int?
    ): Boolean = when (stage) {
        CycleStage.YEARS_ONE_TO_THREE ->
            latestCompletedLength?.let { it !in EARLY_STAGE_CYCLE_RANGE } == true ||
                (currentCycleDay ?: 0) > EARLY_STAGE_CYCLE_RANGE.last
        CycleStage.ESTABLISHED ->
            latestCompletedLength?.let { it !in ESTABLISHED_CYCLE_RANGE } == true ||
                (currentCycleDay ?: 0) > ESTABLISHED_CYCLE_RANGE.last
        else -> false
    }

    private fun noticeForStage(stage: CycleStage): CycleNotice? = when (stage) {
        CycleStage.NOT_SET -> CycleNotice.CYCLE_STAGE_NOT_SET
        CycleStage.FIRST_YEAR -> CycleNotice.FIRST_YEAR_CHANGES_ARE_COMMON
        CycleStage.YEARS_ONE_TO_THREE -> CycleNotice.EARLY_YEARS_CHANGES_ARE_COMMON
        CycleStage.ESTABLISHED -> null
        CycleStage.LONG_TERM_UNEVEN -> CycleNotice.LONG_TERM_UNEVEN
        CycleStage.CHANGING_WITH_AGE -> CycleNotice.CHANGING_WITH_AGE
        CycleStage.PERIODS_STOPPED -> CycleNotice.PERIODS_STOPPED
    }

    companion object {
        private const val YEAR_GAP_DAYS = 365
        private const val LONG_GAP_DAYS = 90
        private const val EARLY_STAGE_BLEEDING_LIMIT_DAYS = 7
        private const val DEFAULT_BLEEDING_LIMIT_DAYS = 8
        private val EARLY_STAGE_CYCLE_RANGE = 21..45
        private val ESTABLISHED_CYCLE_RANGE = 21..35
    }
}
