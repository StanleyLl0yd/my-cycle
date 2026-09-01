package com.sl.mycycle.ui.calendar

import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.DateRange
import com.sl.mycycle.domain.model.FertilityState
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.PeriodState
import com.sl.mycycle.domain.model.Prediction
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDayStateResolverTest {

    private val date = LocalDate.of(2026, 8, 15)

    @Test
    fun onboardingPeriodStartWithoutFlowIsUnspecifiedPeriod() {
        val day = CycleDay(date = date, hasPeriod = true)

        assertEquals(
            PeriodState.CONFIRMED_UNSPECIFIED,
            resolvePeriodState(date, day, prediction(periodDate = date))
        )
    }

    @Test
    fun explicitNoBloodOverridesPredictedPeriod() {
        val day = CycleDay(date = date, hasPeriod = false)

        assertEquals(
            PeriodState.NONE,
            resolvePeriodState(date, day, prediction(periodDate = date))
        )
    }

    @Test
    fun spottingIsNotRenderedAsPeriodBleeding() {
        val day = CycleDay(
            date = date,
            hasPeriod = false,
            flowIntensity = FlowIntensity.SPOTTING
        )

        assertEquals(
            PeriodState.CONFIRMED_SPOTTING,
            resolvePeriodState(date, day, prediction(periodDate = date))
        )
    }

    @Test
    fun recordedFlowMapsToMatchingPeriodState() {
        val expected = mapOf(
            FlowIntensity.LIGHT to PeriodState.CONFIRMED_LIGHT,
            FlowIntensity.MEDIUM to PeriodState.CONFIRMED_MEDIUM,
            FlowIntensity.HEAVY to PeriodState.CONFIRMED_HEAVY
        )

        expected.forEach { (flow, state) ->
            val day = CycleDay(date = date, hasPeriod = true, flowIntensity = flow)
            assertEquals(state, resolvePeriodState(date, day, null))
        }
    }

    @Test
    fun predictionIsShownOnlyWhenThereIsNoSavedObservation() {
        val prediction = prediction(periodDate = date)

        assertEquals(PeriodState.PREDICTED, resolvePeriodState(date, null, prediction))
        assertEquals(
            PeriodState.NONE,
            resolvePeriodState(date, CycleDay(date = date), prediction)
        )
    }

    @Test
    fun confirmedPeriodSuppressesFertilityPrediction() {
        val day = CycleDay(
            date = date,
            hasPeriod = true,
            flowIntensity = FlowIntensity.MEDIUM
        )
        val prediction = prediction(ovulationDate = date, pregnancyDate = date)

        assertEquals(
            FertilityState.NONE,
            resolveFertilityState(date, day, prediction)
        )
    }

    @Test
    fun ovulationPredictionHasPriorityInsidePregnancyWindow() {
        val prediction = prediction(ovulationDate = date, pregnancyDate = date)

        assertEquals(
            FertilityState.OVULATION_PREDICTED,
            resolveFertilityState(date, null, prediction)
        )
    }

    @Test
    fun explicitNoBloodDoesNotHideFertilityPrediction() {
        val day = CycleDay(date = date, hasPeriod = false)
        val prediction = prediction(pregnancyDate = date)

        assertEquals(
            FertilityState.FERTILE_PREDICTED,
            resolveFertilityState(date, day, prediction)
        )
    }

    private fun prediction(
        periodDate: LocalDate? = null,
        ovulationDate: LocalDate? = null,
        pregnancyDate: LocalDate? = null
    ) = Prediction(
        nextPeriodStartWindow = periodDate?.let { DateRange(it, it) },
        possiblePregnancyWindow = pregnancyDate?.let { DateRange(it, it) },
        possibleOvulationWindow = ovulationDate?.let { DateRange(it, it) },
        basedOnCycles = 3,
        highlyVariable = false,
        outsideCommonRange = false,
        stage = CycleStage.ESTABLISHED
    )
}
