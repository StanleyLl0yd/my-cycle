package com.example.mycycle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.preferences.UserPreferencesRepository
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.model.CycleDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OnboardingState(
    val currentStep: Int = 0,
    val lastPeriodDate: LocalDate = LocalDate.now().minusDays(14),
    val cycleLength: Int = 28,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun nextStep() {
        _state.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun setLastPeriodDate(date: LocalDate) {
        _state.update { it.copy(lastPeriodDate = date) }
    }

    fun setCycleLength(length: Int) {
        _state.update { it.copy(cycleLength = length.coerceIn(21, 45)) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val currentState = _state.value

            // Save initial period day
            cycleDayRepository.save(
                CycleDay(
                    date = currentState.lastPeriodDate,
                    hasPeriod = true
                )
            )

            // Save preferences
            preferencesRepository.completeOnboarding(
                lastPeriodDate = currentState.lastPeriodDate,
                cycleLength = currentState.cycleLength
            )

            _state.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
}
