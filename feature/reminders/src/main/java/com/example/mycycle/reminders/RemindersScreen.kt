package com.example.mycycle.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.ReminderRepository
import com.example.mycycle.model.Reminder
import com.example.mycycle.model.ReminderType
import com.example.mycycle.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    val state = reminderRepository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(reminderId: String, enabled: Boolean) {
        viewModelScope.launch { reminderRepository.toggle(reminderId, enabled) }
    }

    fun addReminder() {
        viewModelScope.launch {
            reminderRepository.addReminder(ReminderType.OVULATION, LocalTime.of(9, 0))
        }
    }
}

@Composable
fun RemindersScreen(titleRes: Int, viewModel: RemindersViewModel = hiltViewModel()) {
    val reminders by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(id = titleRes)) })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reminders) { reminder -> ReminderRow(reminder, onToggle = { enabled -> viewModel.toggle(reminder.id, enabled) }) }
        }
        TextButton(onClick = viewModel::addReminder, modifier = Modifier.align(Alignment.End).padding(16.dp)) {
            Text(text = stringResource(id = R.string.reminders_add_button))
        }
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onToggle: (Boolean) -> Unit) {
    val typeLabel = stringResource(
        when (reminder.type) {
            ReminderType.CYCLE_START -> R.string.reminder_type_cycle_start
            ReminderType.OVULATION -> R.string.reminder_type_ovulation
            ReminderType.MEDICATION -> R.string.reminder_type_medication
            ReminderType.GENERAL -> R.string.reminder_type_general
        }
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = typeLabel, fontWeight = FontWeight.SemiBold)
                Text(text = stringResource(id = R.string.reminders_time, reminder.time))
            }
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
        }
    }
}
