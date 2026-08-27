package com.example.mycycle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.CycleStage
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val currentStep: Int = 0,
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val lastPeriodDate: LocalDate,
    val cycleLength: Int = 28,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(
            lastPeriodDate = LocalDate.now(clock).minusDays(14)
        )
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun nextStep() {
        _state.update { current ->
            if (current.currentStep == 1 && current.cycleStage == CycleStage.NOT_SET) {
                current
            } else {
                current.copy(currentStep = (current.currentStep + 1).coerceAtMost(3))
            }
        }
    }

    fun previousStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun setCycleStage(stage: CycleStage) {
        if (stage == CycleStage.NOT_SET) return
        _state.update { current ->
            current.copy(
                cycleStage = stage,
                cycleLength = when (stage) {
                    CycleStage.FIRST_YEAR -> 32
                    else -> current.cycleLength
                }
            )
        }
    }

    fun setLastPeriodDate(date: LocalDate) {
        val today = LocalDate.now(clock)
        val safeDate = if (date.isAfter(today)) today else date
        _state.update { it.copy(lastPeriodDate = safeDate) }
    }

    fun setCycleLength(length: Int) {
        _state.update { it.copy(cycleLength = length.coerceIn(15, 90)) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.cycleStage == CycleStage.NOT_SET) return@launch

            _state.update { it.copy(isLoading = true) }

            val periodsStopped = currentState.cycleStage == CycleStage.PERIODS_STOPPED
            val savedLastPeriodDate = currentState.lastPeriodDate.takeUnless { periodsStopped }

            if (savedLastPeriodDate != null) {
                cycleDayRepository.save(
                    CycleDay(
                        date = savedLastPeriodDate,
                        hasPeriod = true
                    )
                )
            }

            preferencesRepository.completeOnboarding(
                lastPeriodDate = savedLastPeriodDate,
                cycleLength = currentState.cycleLength,
                cycleStage = currentState.cycleStage
            )

            _state.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
}
