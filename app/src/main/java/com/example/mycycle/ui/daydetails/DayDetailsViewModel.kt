package com.example.mycycle.ui.daydetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.repository.CycleDayRepository
import com.example.mycycle.domain.model.CycleDay
import com.example.mycycle.domain.model.FlowIntensity
import com.example.mycycle.domain.model.Mood
import com.example.mycycle.domain.model.Symptom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayDetailsState(
    val date: LocalDate = LocalDate.now(),
    val hasPeriod: Boolean = false,
    val flowIntensity: FlowIntensity? = null,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

class DayDetailsViewModel(
    private val dateString: String,
    private val cycleDayRepository: CycleDayRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailsState())
    val state: StateFlow<DayDetailsState> = _state.asStateFlow()

    init {
        loadDay()
    }

    private fun loadDay() {
        viewModelScope.launch {
            val date = LocalDate.parse(dateString)
            val existingDay = cycleDayRepository.getByDate(date)

            _state.update {
                DayDetailsState(
                    date = date,
                    hasPeriod = existingDay?.hasPeriod ?: false,
                    flowIntensity = existingDay?.flowIntensity,
                    mood = existingDay?.mood,
                    symptoms = existingDay?.symptoms ?: emptySet(),
                    notes = existingDay?.notes ?: "",
                    isLoading = false
                )
            }
        }
    }

    fun setFlowIntensity(intensity: FlowIntensity?) {
        _state.update {
            it.copy(
                flowIntensity = intensity,
                hasPeriod = intensity != null
            )
        }
    }

    fun setMood(mood: Mood?) {
        _state.update { it.copy(mood = mood) }
    }

    fun toggleSymptom(symptom: Symptom) {
        _state.update { currentState ->
            val newSymptoms = if (symptom in currentState.symptoms) {
                currentState.symptoms - symptom
            } else {
                currentState.symptoms + symptom
            }
            currentState.copy(symptoms = newSymptoms)
        }
    }

    fun setNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun save() {
        viewModelScope.launch {
            val currentState = _state.value
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
            _state.update { it.copy(isSaved = true) }
        }
    }
}
