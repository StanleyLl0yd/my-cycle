package com.silverlightning.mycycle.ui.daydetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.FlowIntensity
import com.silverlightning.mycycle.domain.model.Mood
import com.silverlightning.mycycle.domain.model.Symptom
import com.silverlightning.mycycle.util.ClockProvider
import com.silverlightning.mycycle.util.currentDateFlow
import com.silverlightning.mycycle.util.runSuspendCatching
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DayDetailsState(
    val date: LocalDate = LocalDate.MIN,
    val hasPeriod: Boolean = false,
    val flowIntensity: FlowIntensity? = null,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val notes: String = "",
    val isFutureDate: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val isDirty: Boolean = false,
    val isSaved: Boolean = false
)

class DayDetailsViewModel(
    private val dateString: String,
    private val cycleDayRepository: CycleDayRepository,
    private val clockProvider: ClockProvider
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailsState())
    val state: StateFlow<DayDetailsState> = _state.asStateFlow()

    init {
        loadDay()
    }

    private fun loadDay() {
        viewModelScope.launch {
            val date = LocalDate.parse(dateString)
            combine(
                cycleDayRepository.observeByDate(date),
                currentDateFlow(clockProvider)
            ) { existingDay, today ->
                Pair(existingDay, today)
            }.collect { (existingDay, today) ->
                _state.update { current ->
                    resolveDayDetailsRefresh(
                        current = current,
                        existingDay = existingDay,
                        date = date,
                        today = today
                    )
                }
            }
        }
    }

    fun setFlowIntensity(intensity: FlowIntensity?) {
        if (_state.value.isSaving) return
        _state.update {
            it.copy(
                flowIntensity = intensity,
                hasPeriod = intensity != null && intensity != FlowIntensity.SPOTTING,
                hasSaveError = false,
                isDirty = true
            )
        }
    }

    fun setMood(mood: Mood?) {
        if (_state.value.isSaving) return
        _state.update { it.copy(mood = mood, hasSaveError = false, isDirty = true) }
    }

    fun toggleSymptom(symptom: Symptom) {
        if (_state.value.isSaving) return
        _state.update { currentState ->
            val newSymptoms = if (symptom in currentState.symptoms) {
                currentState.symptoms - symptom
            } else {
                currentState.symptoms + symptom
            }
            currentState.copy(
                symptoms = newSymptoms,
                hasSaveError = false,
                isDirty = true
            )
        }
    }

    fun setNotes(notes: String) {
        if (_state.value.isSaving) return
        _state.update {
            it.copy(notes = notes, hasSaveError = false, isDirty = true)
        }
    }

    fun save() {
        if (_state.value.isSaving) return

        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.date.isAfter(clockProvider.today())) return@launch

            _state.update { it.copy(isSaving = true, hasSaveError = false) }

            val result = runSuspendCatching {
                cycleDayRepository.save(
                    CycleDay(
                        date = currentState.date,
                        hasPeriod = currentState.hasPeriod,
                        flowIntensity = currentState.flowIntensity,
                        mood = currentState.mood,
                        symptoms = currentState.symptoms,
                        notes = currentState.notes.takeIf { it.isNotBlank() }
                    )
                )
            }

            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        hasSaveError = false,
                        isDirty = false,
                        isSaved = true
                    )
                }
            } else {
                _state.update { it.copy(isSaving = false, hasSaveError = true) }
            }
        }
    }
}
