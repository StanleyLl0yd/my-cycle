package com.example.mycycle.domain.engine

import com.example.mycycle.domain.model.CycleNotice
import com.example.mycycle.domain.model.CycleStage

class CycleNoticeEvaluator {

    fun evaluate(
        stage: CycleStage,
        latestCompletedLength: Int?,
        currentCycleDay: Int?,
        currentPeriodLength: Int?,
        isPeriodToday: Boolean,
        bleedingToday: Boolean
    ): CycleNotice? {
        if (
            bleedingToday &&
            (
                stage == CycleStage.PERIODS_STOPPED ||
                    latestCompletedLength?.let { it >= 365 } == true ||
                    (currentCycleDay ?: 0) >= 365
            )
        ) {
            return CycleNotice.BLEEDING_AFTER_YEAR_GAP
        }

        val longBleedingLimit = when (stage) {
            CycleStage.FIRST_YEAR,
            CycleStage.YEARS_ONE_TO_THREE -> 7
            else -> 8
        }
        if (
            isPeriodToday &&
            currentPeriodLength != null &&
            currentPeriodLength > longBleedingLimit
        ) {
            return CycleNotice.LONG_BLEEDING
        }

        val gapDays = maxOf(
            latestCompletedLength ?: 0,
            currentCycleDay ?: 0
        )

        if (
            stage in setOf(CycleStage.FIRST_YEAR, CycleStage.YEARS_ONE_TO_THREE) &&
            gapDays >= 90
        ) {
            return CycleNotice.THREE_MONTH_GAP
        }

        if (
            stage in setOf(CycleStage.ESTABLISHED, CycleStage.LONG_TERM_UNEVEN) &&
            gapDays >= 90
        ) {
            return CycleNotice.LONG_UNEXPLAINED_GAP
        }

        val outsideCommonRange = when (stage) {
            CycleStage.YEARS_ONE_TO_THREE ->
                latestCompletedLength?.let { it !in 21..45 } == true ||
                    (currentCycleDay ?: 0) > 45
            CycleStage.ESTABLISHED ->
                latestCompletedLength?.let { it !in 21..35 } == true ||
                    (currentCycleDay ?: 0) > 35
            else -> false
        }
        if (outsideCommonRange) {
            return CycleNotice.OUTSIDE_COMMON_RANGE
        }

        return when (stage) {
            CycleStage.NOT_SET -> CycleNotice.CYCLE_STAGE_NOT_SET
            CycleStage.FIRST_YEAR -> CycleNotice.FIRST_YEAR_CHANGES_ARE_COMMON
            CycleStage.YEARS_ONE_TO_THREE -> CycleNotice.EARLY_YEARS_CHANGES_ARE_COMMON
            CycleStage.ESTABLISHED -> null
            CycleStage.LONG_TERM_UNEVEN -> CycleNotice.LONG_TERM_UNEVEN
            CycleStage.CHANGING_WITH_AGE -> CycleNotice.CHANGING_WITH_AGE
            CycleStage.PERIODS_STOPPED -> CycleNotice.PERIODS_STOPPED
        }
    }
}
