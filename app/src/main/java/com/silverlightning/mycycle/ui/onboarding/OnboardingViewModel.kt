package com.silverlightning.mycycle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverlightning.mycycle.data.preferences.UserPreferencesRepository
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.CycleStage
import com.silverlightning.mycycle.util.ClockProvider
import com.silverlightning.mycycle.util.runSuspendCatching
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OnboardingState(
    val currentStep: Int = 0,
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val lastPeriodDate: LocalDate,
    val cycleLength: Int = 28,
    val isLoading: Boolean = false,
    val hasSaveError: Boolean = false,
    val isComplete: Boolean = false
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository,
    private val clockProvider: ClockProvider
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingState(
            lastPeriodDate = clockProvider.today().minusDays(14)
        )
    )
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun nextStep() {
        if (_state.value.isLoading) return
        _state.update { current ->
            if (current.currentStep == 1 && current.cycleStage == CycleStage.NOT_SET) {
                current
            } else {
                current.copy(
                    currentStep = (current.currentStep + 1).coerceAtMost(3),
                    hasSaveError = false
                )
            }
        }
    }

    fun previousStep() {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(
                currentStep = (it.currentStep - 1).coerceAtLeast(0),
                hasSaveError = false
            )
        }
    }

    fun setCycleStage(stage: CycleStage) {
        if (_state.value.isLoading || stage == CycleStage.NOT_SET) return
        _state.update { current ->
            current.copy(
                cycleStage = stage,
                cycleLength = when (stage) {
                    CycleStage.FIRST_YEAR -> 32
                    else -> current.cycleLength
                },
                hasSaveError = false
            )
        }
    }

    fun setLastPeriodDate(date: LocalDate) {
        if (_state.value.isLoading) return
        val today = clockProvider.today()
        val safeDate = if (date.isAfter(today)) today else date
        _state.update { it.copy(lastPeriodDate = safeDate, hasSaveError = false) }
    }

    fun setCycleLength(length: Int) {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(
                cycleLength = length.coerceIn(15, 90),
                hasSaveError = false
            )
        }
    }

    fun completeOnboarding() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.cycleStage == CycleStage.NOT_SET) return@launch

            _state.update { it.copy(isLoading = true, hasSaveError = false) }

            val periodsStopped = currentState.cycleStage == CycleStage.PERIODS_STOPPED
            val savedLastPeriodDate = currentState.lastPeriodDate.takeUnless { periodsStopped }

            val result = runSuspendCatching {
                val previousDay = savedLastPeriodDate?.let { cycleDayRepository.getByDate(it) }

                if (savedLastPeriodDate != null) {
                    cycleDayRepository.save(
                        CycleDay(
                            date = savedLastPeriodDate,
                            hasPeriod = true
                        )
                    )
                }

                try {
                    preferencesRepository.completeOnboarding(
                        lastPeriodDate = savedLastPeriodDate,
                        cycleLength = currentState.cycleLength,
                        cycleStage = currentState.cycleStage
                    )
                } catch (error: Throwable) {
                    if (savedLastPeriodDate != null) {
                        withContext(NonCancellable) {
                            try {
                                if (previousDay != null) {
                                    cycleDayRepository.save(previousDay)
                                } else {
                                    cycleDayRepository.delete(savedLastPeriodDate)
                                }
                            } catch (rollbackError: Exception) {
                                error.addSuppressed(rollbackError)
                            }
                        }
                    }
                    throw error
                }
            }

            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, isComplete = true) }
            } else {
                _state.update { it.copy(isLoading = false, hasSaveError = true) }
            }
        }
    }
}
