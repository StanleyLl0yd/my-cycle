package com.example.mycycle.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.LogRepository
import com.example.mycycle.model.BleedingLevel
import com.example.mycycle.model.LogEntry
import com.example.mycycle.model.Mood
import com.example.mycycle.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class LogUiState(
    val entries: List<LogEntry> = emptyList(),
    val pending: LogEntryDraft = LogEntryDraft(),
)

internal data class LogEntryDraft(
    val date: LocalDate = LocalDate.now(),
    val bleeding: BleedingLevel? = BleedingLevel.LIGHT,
    val mood: Mood? = Mood.NEUTRAL,
    val symptoms: Set<String> = emptySet(),
    val notes: String = "",
)

@HiltViewModel
internal class LogViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val draft = MutableStateFlow(LogEntryDraft())

    val state = combine(logRepository.logs, draft) { entries, draft ->
        LogUiState(entries = entries, pending = draft)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun toggleSymptom(symptom: String) {
        draft.value = draft.value.copy(
            symptoms = draft.value.symptoms.toggle(symptom)
        )
    }

    fun updateNotes(notes: String) {
        draft.value = draft.value.copy(notes = notes)
    }

    fun updateBleeding(level: Float) {
        val mapped = when {
            level < 0.34f -> BleedingLevel.LIGHT
            level < 0.67f -> BleedingLevel.MEDIUM
            else -> BleedingLevel.HEAVY
        }
        draft.value = draft.value.copy(bleeding = mapped)
    }

    fun updateMood(mood: Mood) {
        draft.value = draft.value.copy(mood = mood)
    }

    fun saveEntry() {
        val currentDraft = draft.value
        viewModelScope.launch {
            logRepository.addOrUpdate(
                LogEntry(
                    id = "", // replaced in repository
                    date = currentDraft.date,
                    bleedingLevel = currentDraft.bleeding,
                    symptoms = currentDraft.symptoms.toList(),
                    mood = currentDraft.mood,
                    temperatureCelsius = null,
                    weightKg = null,
                    notes = currentDraft.notes.ifBlank { null }
                )
            )
            draft.value = LogEntryDraft()
        }
    }
}

@Composable
fun LogScreen(titleRes: Int, viewModel: LogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(id = titleRes)) })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LogEntryEditor(state.pending, viewModel::updateBleeding, viewModel::toggleSymptom, viewModel::updateMood, viewModel::updateNotes, viewModel::saveEntry) }
            items(state.entries) { entry -> LogCard(entry) }
        }
    }
}

@Composable
private fun LogEntryEditor(
    draft: LogEntryDraft,
    onBleedingChange: (Float) -> Unit,
    onSymptomToggle: (String) -> Unit,
    onMoodChange: (Mood) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.log_new_entry_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(id = R.string.log_bleeding_label))
            Slider(value = draft.bleeding?.asProgress() ?: 0f, onValueChange = onBleedingChange)
            Text(text = stringResource(id = R.string.log_symptoms_label))
            val symptoms = listOf("cramps", "bloating", "fatigue", "headache")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                symptoms.forEach { symptom ->
                    FilterChip(
                        selected = symptom in draft.symptoms,
                        onClick = { onSymptomToggle(symptom) },
                        label = { Text(symptom) }
                    )
                }
            }
            Text(text = stringResource(id = R.string.log_mood_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.values().forEach { mood ->
                    FilterChip(selected = draft.mood == mood, onClick = { onMoodChange(mood) }, label = { Text(mood.name) })
                }
            }
            OutlinedTextField(
                value = draft.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.log_notes_label)) }
            )
            Button(onClick = onSave) { Text(stringResource(id = R.string.log_save_button)) }
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = stringResource(id = R.string.log_entry_date, entry.date), fontWeight = FontWeight.SemiBold)
            entry.bleedingLevel?.let { Text(stringResource(id = R.string.log_entry_bleeding, it.name.lowercase())) }
            if (entry.symptoms.isNotEmpty()) {
                Text(text = stringResource(id = R.string.log_entry_symptoms, entry.symptoms.joinToString()))
            }
            entry.mood?.let { Text(text = stringResource(id = R.string.log_entry_mood, it.name.lowercase())) }
            entry.notes?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private fun BleedingLevel.asProgress(): Float = when (this) {
    BleedingLevel.LIGHT -> 0.2f
    BleedingLevel.MEDIUM -> 0.5f
    BleedingLevel.HEAVY -> 0.9f
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
