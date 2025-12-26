package com.example.mycycle.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.mycycle.domain.model.CyclePhase
import com.example.mycycle.domain.model.FlowIntensity

object CycleColors {

    fun getPhaseColor(phase: CyclePhase): Color = when (phase) {
        CyclePhase.MENSTRUAL -> PhaseMenstrual
        CyclePhase.FOLLICULAR -> PhaseFollicular
        CyclePhase.OVULATORY -> PhaseOvulatory
        CyclePhase.LUTEAL -> PhaseLuteal
    }

    fun getPhaseGradient(phase: CyclePhase): Brush = when (phase) {
        CyclePhase.MENSTRUAL -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFEF0EB),
                Color(0xFFFDE8E2)
            )
        )
        CyclePhase.FOLLICULAR -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF9EF),
                Color(0xFFFFF5E6)
            )
        )
        CyclePhase.OVULATORY -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F7F4),
                Color(0xFFE8F3EE)
            )
        )
        CyclePhase.LUTEAL -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8F4FB),
                Color(0xFFF3EDF8)
            )
        )
    }

    fun getPeriodColor(intensity: FlowIntensity?): Color = when (intensity) {
        FlowIntensity.SPOTTING -> PeriodLight
        FlowIntensity.LIGHT -> PeriodMedium
        FlowIntensity.MEDIUM -> PeriodStrong
        FlowIntensity.HEAVY -> PeriodHeavy
        null -> PeriodMedium
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F3),
            Color(0xFFFFFBF7),
            Color(0xFFFEF7F0)
        )
    )
}
