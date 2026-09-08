package com.sl.mycycle.reminder

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.DateRange
import com.sl.mycycle.domain.model.Prediction
import com.sl.mycycle.domain.model.PredictionMethod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDecisionEngineTest {

    private val engine = ReminderDecisionEngine()
    private val today = LocalDate.of(2026, 9, 8)

    @Test
    fun skipsReminderWhenTodayIsAlreadyRecorded() {
        assertEquals(
            ReminderKind.NONE,
            engine.decide(today, CycleDay(today), prediction())
        )
    }

    @Test
    fun usesWindowReminderWhenPredictedRangeIsClose() {
        assertEquals(
            ReminderKind.PERIOD_WINDOW,
            engine.decide(today, null, prediction())
        )
    }

    @Test
    fun fallsBackToDiaryReminderOutsidePredictionRange() {
        assertEquals(
            ReminderKind.DIARY,
            engine.decide(today.minusDays(10), null, prediction())
        )
    }

    private fun prediction(): Prediction = Prediction(
        nextPeriodStartWindow = DateRange(today.plusDays(1), today.plusDays(5)),
        expectedPeriodLength = 5,
        possiblePregnancyWindow = null,
        possibleOvulationWindow = null,
        confidence = 0.5f,
        basedOnCycles = 3,
        method = PredictionMethod.WEIGHTED_AVERAGE,
        estimatedCycleLength = 28,
        highlyVariable = false,
        outsideCommonRange = false,
        stage = CycleStage.ESTABLISHED
    )
}
