package com.sl.mycycle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.util.ClockProvider
import com.sl.mycycle.util.runSuspendCatching
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_CYCLE_LENGTH = 28
private const val INITIAL_LAST_PERIOD_OFFSET_DAYS = 14L
private const val FINAL_ONBOARDING_STEP = 3
private const val FIRST_YEAR_DEFAULT_CYCLE_LENGTH = 32
private const val MIN_CYCLE_LENGTH = 15
private const val MAX_CYCLE_LENGTH = 90

data class OnboardingState(
    val currentStep: Int = 0,
    val cycleStage: CycleStage = CycleStage.NOT_SET,
    val lastPeriodDate: LocalDate,
    val cycleLength: Int = DEFAULT_CYCLE_LENGTH,
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
            lastPeriodDate = clockProvider.today().minusDays(INITIAL_LAST_PERIOD_OFFSET_DAYS)
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
                    currentStep = (current.currentStep + 1).coerceAtMost(FINAL_ONBOARDING_STEP),
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
                    CycleStage.FIRST_YEAR -> FIRST_YEAR_DEFAULT_CYCLE_LENGTH
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
                cycleLength = length.coerceIn(MIN_CYCLE_LENGTH, MAX_CYCLE_LENGTH),
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
